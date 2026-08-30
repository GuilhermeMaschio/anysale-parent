package com.anysale.catalog.application;

import com.anysale.catalog.adapters.out.persistence.CatalogIntegrationRepository;
import com.anysale.catalog.adapters.out.persistence.CatalogSyncExecutionRepository;
import com.anysale.catalog.adapters.out.persistence.ProductRepository;
import com.anysale.catalog.application.connector.CatalogProviderConnector;
import com.anysale.catalog.application.mapper.FieldMapper;
import com.anysale.catalog.domain.model.CatalogIntegration;
import com.anysale.catalog.domain.model.CatalogSyncExecution;
import com.anysale.catalog.domain.model.Product;
import com.anysale.catalog.security.CredentialEncryptionService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CatalogSyncProcessor {
    private static final Logger log = LoggerFactory.getLogger(CatalogSyncProcessor.class);
    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB limit

    private final CatalogIntegrationRepository integrationRepository;
    private final CatalogSyncExecutionRepository executionRepository;
    private final ProductRepository productRepository;
    private final List<CatalogProviderConnector> connectors;
    private final FieldMapper fieldMapper;
    private final CredentialEncryptionService encryptionService;
    private final GridFsTemplate gridFsTemplate;
    private final HttpClient imageHttpClient;

    public CatalogSyncProcessor(
            CatalogIntegrationRepository integrationRepository,
            CatalogSyncExecutionRepository executionRepository,
            ProductRepository productRepository,
            List<CatalogProviderConnector> connectors,
            FieldMapper fieldMapper,
            CredentialEncryptionService encryptionService,
            GridFsTemplate gridFsTemplate
    ) {
        this.integrationRepository = integrationRepository;
        this.executionRepository = executionRepository;
        this.productRepository = productRepository;
        this.connectors = connectors;
        this.fieldMapper = fieldMapper;
        this.encryptionService = encryptionService;
        this.gridFsTemplate = gridFsTemplate;
        this.imageHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Async
    public void executeSync(String integrationId, String tenantId) {
        Optional<CatalogIntegration> opt = integrationRepository.findByIdAndTenantId(integrationId, tenantId);
        if (opt.isEmpty()) {
            log.error("Integration {} not found for tenant {}", integrationId, tenantId);
            return;
        }

        CatalogIntegration integration = opt.get();
        CatalogSyncExecution execution = new CatalogSyncExecution();
        execution.setTenantId(tenantId);
        execution.setIntegrationId(integrationId);
        execution.setStatus("IN_PROGRESS");
        execution.setStartedAt(Instant.now());
        execution = executionRepository.save(execution);

        try {
            CatalogProviderConnector connector = connectors.stream()
                    .filter(c -> c.supports(integration.getProviderType()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No connector supporting provider " + integration.getProviderType()));

            String decryptedSecret = encryptionService.decrypt(integration.getEncryptedCredentials());
            List<Map<String, Object>> rawProducts = connector.fetchRawProducts(integration, decryptedSecret, 1, 500);

            int created = 0, updated = 0, skipped = 0, errors = 0;

            for (Map<String, Object> rawItem : rawProducts) {
                try {
                    FieldMapper.MappingResult mappingResult = fieldMapper.mapToProduct(
                            rawItem, integration.getFieldMapping(), tenantId, integration.getId()
                    );

                    if (!mappingResult.isValid()) {
                        errors++;
                        execution.getErrorSummary().add("Skipped product: " + String.join(", ", mappingResult.validationErrors()));
                        continue;
                    }

                    Product mappedProduct = mappingResult.product();
                    String extId = mappedProduct.getExternalProductId();
                    String sku = mappedProduct.getSku();

                    Optional<Product> existingOpt = productRepository.findByTenantIdAndSourceAndExternalProductIdAndDeletedAtIsNull(tenantId, integration.getId(), extId);
                    if (existingOpt.isEmpty() && integration.isSkuFallbackEnabled() && sku != null) {
                        existingOpt = productRepository.findByTenantIdAndSkuAndDeletedAtIsNull(tenantId, sku);
                    }

                    String strategy = integration.getConflictStrategy() != null ? integration.getConflictStrategy() : "EXTERNAL_WINS";

                    if (existingOpt.isPresent()) {
                        Product existing = existingOpt.get();
                        if ("LOCAL_WINS".equalsIgnoreCase(strategy) || "REVIEW_REQUIRED".equalsIgnoreCase(strategy)) {
                            skipped++;
                            continue;
                        }

                        // EXTERNAL_WINS: update fields
                        existing.setTitle(mappedProduct.getTitle());
                        existing.setDescription(mappedProduct.getDescription());
                        existing.setCategory(mappedProduct.getCategory());
                        existing.setPrice(mappedProduct.getPrice());
                        existing.setCurrency(mappedProduct.getCurrency());
                        existing.setStockQuantity(mappedProduct.getStockQuantity());
                        existing.setReorderPoint(mappedProduct.getReorderPoint());
                        existing.setAvailable(mappedProduct.isAvailable());
                        existing.setLastSyncedAt(Instant.now());
                        existing.setUpdatedAt(Instant.now());

                        tryDownloadImage(mappingResult.imageUrl(), existing);
                        productRepository.save(existing);
                        updated++;
                    } else {
                        // Create new product
                        mappedProduct.setCreatedAt(Instant.now());
                        mappedProduct.setUpdatedAt(Instant.now());
                        mappedProduct.setLastSyncedAt(Instant.now());

                        tryDownloadImage(mappingResult.imageUrl(), mappedProduct);
                        productRepository.save(mappedProduct);
                        created++;
                    }
                } catch (Exception e) {
                    errors++;
                    execution.getErrorSummary().add("Error processing item: " + e.getMessage());
                }
            }

            execution.setCreatedCount(created);
            execution.setUpdatedCount(updated);
            execution.setSkippedCount(skipped);
            execution.setErrorCount(errors);
            execution.setStatus("COMPLETED");
            execution.setFinishedAt(Instant.now());

            integration.setLastSyncAt(Instant.now());
            integration.setLastSyncStatus("COMPLETED");
            integrationRepository.save(integration);

        } catch (Exception e) {
            log.error("Execution failed for integration {}", integrationId, e);
            execution.setStatus("FAILED");
            execution.setFinishedAt(Instant.now());
            execution.getErrorSummary().add("Sync execution failed: " + e.getMessage());

            integration.setLastSyncAt(Instant.now());
            integration.setLastSyncStatus("ERROR");
            integrationRepository.save(integration);
        } finally {
            executionRepository.save(execution);
        }
    }

    private void tryDownloadImage(String imageUrl, Product product) {
        if (imageUrl == null || !imageUrl.startsWith("https://")) return;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = imageHttpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200 && response.body().length > 0 && response.body().length <= MAX_IMAGE_SIZE_BYTES) {
                String contentType = response.headers().firstValue("Content-Type").orElse("image/jpeg");
                if (contentType.startsWith("image/")) {
                    ObjectId gridFsId = gridFsTemplate.store(
                            new ByteArrayInputStream(response.body()),
                            product.getSku() + "_ext_img",
                            contentType
                    );
                    product.setImageId(gridFsId.toHexString());
                    product.setImageContentType(contentType);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to download image from {} for product {}", imageUrl, product.getSku(), e);
        }
    }
}
