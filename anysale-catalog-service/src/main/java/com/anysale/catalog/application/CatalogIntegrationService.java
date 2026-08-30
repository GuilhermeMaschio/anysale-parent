package com.anysale.catalog.application;

import com.anysale.catalog.adapters.in.rest.dto.*;
import com.anysale.catalog.adapters.out.persistence.CatalogIntegrationRepository;
import com.anysale.catalog.adapters.out.persistence.CatalogSyncExecutionRepository;
import com.anysale.catalog.application.connector.CatalogProviderConnector;
import com.anysale.catalog.application.connector.ConnectionTestResult;
import com.anysale.catalog.application.mapper.FieldMapper;
import com.anysale.catalog.domain.model.CatalogIntegration;
import com.anysale.catalog.domain.model.CatalogSyncExecution;
import com.anysale.catalog.domain.model.Product;
import com.anysale.catalog.security.CredentialEncryptionService;
import com.anysale.catalog.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CatalogIntegrationService {

    private final CatalogIntegrationRepository repository;
    private final CatalogSyncExecutionRepository executionRepository;
    private final TenantContext tenantContext;
    private final CredentialEncryptionService encryptionService;
    private final List<CatalogProviderConnector> connectors;
    private final FieldMapper fieldMapper;
    private final CatalogSyncProcessor syncProcessor;

    public CatalogIntegrationService(
            CatalogIntegrationRepository repository,
            CatalogSyncExecutionRepository executionRepository,
            TenantContext tenantContext,
            CredentialEncryptionService encryptionService,
            List<CatalogProviderConnector> connectors,
            FieldMapper fieldMapper,
            CatalogSyncProcessor syncProcessor
    ) {
        this.repository = repository;
        this.executionRepository = executionRepository;
        this.tenantContext = tenantContext;
        this.encryptionService = encryptionService;
        this.connectors = connectors;
        this.fieldMapper = fieldMapper;
        this.syncProcessor = syncProcessor;
    }

    public CatalogIntegrationResponse create(CatalogIntegrationRequest request) {
        String tenantId = tenantContext.tenantId();
        CatalogIntegration integration = new CatalogIntegration();
        integration.setTenantId(tenantId);
        integration.setName(request.name());
        integration.setProviderType(request.providerType() != null ? request.providerType() : "GENERIC_REST");
        integration.setBaseUrl(request.baseUrl());
        integration.setStatus(request.status() != null ? request.status() : "ACTIVE");
        integration.setAuthType(request.authType() != null ? request.authType() : "NONE");
        integration.setApiKeyHeaderName(request.apiKeyHeaderName());
        integration.setApiKeyQueryName(request.apiKeyQueryName());
        integration.setSyncMode(request.syncMode() != null ? request.syncMode() : "MANUAL");
        integration.setSchedule(request.schedule());
        integration.setFieldMapping(request.fieldMapping());
        integration.setConflictStrategy(request.conflictStrategy() != null ? request.conflictStrategy() : "EXTERNAL_WINS");
        integration.setSkuFallbackEnabled(request.skuFallbackEnabled() == null || request.skuFallbackEnabled());

        if (request.secret() != null && !request.secret().isBlank()) {
            integration.setEncryptedCredentials(encryptionService.encrypt(request.secret()));
        }

        integration.setCreatedAt(Instant.now());
        integration.setUpdatedAt(Instant.now());

        CatalogIntegration saved = repository.save(integration);
        return toResponse(saved);
    }

    public CatalogIntegrationResponse update(String id, CatalogIntegrationRequest request) {
        String tenantId = tenantContext.tenantId();
        CatalogIntegration integration = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Integration not found"));

        integration.setName(request.name());
        if (request.providerType() != null) integration.setProviderType(request.providerType());
        integration.setBaseUrl(request.baseUrl());
        if (request.status() != null) integration.setStatus(request.status());
        if (request.authType() != null) integration.setAuthType(request.authType());
        integration.setApiKeyHeaderName(request.apiKeyHeaderName());
        integration.setApiKeyQueryName(request.apiKeyQueryName());
        if (request.syncMode() != null) integration.setSyncMode(request.syncMode());
        integration.setSchedule(request.schedule());
        if (request.fieldMapping() != null) integration.setFieldMapping(request.fieldMapping());
        if (request.conflictStrategy() != null) integration.setConflictStrategy(request.conflictStrategy());
        if (request.skuFallbackEnabled() != null) integration.setSkuFallbackEnabled(request.skuFallbackEnabled());

        if (request.secret() != null && !request.secret().isBlank()) {
            integration.setEncryptedCredentials(encryptionService.encrypt(request.secret()));
        }

        integration.setUpdatedAt(Instant.now());
        CatalogIntegration saved = repository.save(integration);
        return toResponse(saved);
    }

    public List<CatalogIntegrationResponse> list() {
        String tenantId = tenantContext.tenantId();
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    public CatalogIntegrationResponse get(String id) {
        String tenantId = tenantContext.tenantId();
        CatalogIntegration integration = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Integration not found"));
        return toResponse(integration);
    }

    public void delete(String id) {
        String tenantId = tenantContext.tenantId();
        CatalogIntegration integration = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Integration not found"));
        repository.delete(integration);
    }

    public TestConnectionResponse testConnection(String id) {
        String tenantId = tenantContext.tenantId();
        CatalogIntegration integration = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Integration not found"));

        CatalogProviderConnector connector = findConnector(integration.getProviderType());
        String decryptedSecret = encryptionService.decrypt(integration.getEncryptedCredentials());
        ConnectionTestResult result = connector.testConnection(integration, decryptedSecret);

        return new TestConnectionResponse(result.success(), result.statusCode(), result.message());
    }

    public CatalogPreviewResponse preview(String id) {
        String tenantId = tenantContext.tenantId();
        CatalogIntegration integration = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Integration not found"));

        CatalogProviderConnector connector = findConnector(integration.getProviderType());
        String decryptedSecret = encryptionService.decrypt(integration.getEncryptedCredentials());
        List<Map<String, Object>> rawItems = connector.fetchRawProducts(integration, decryptedSecret, 1, 10);

        List<CatalogPreviewResponse.PreviewItem> previewItems = rawItems.stream()
                .map(raw -> {
                    FieldMapper.MappingResult res = fieldMapper.mapToProduct(raw, integration.getFieldMapping(), tenantId, integration.getId());
                    Product p = res.product();
                    return new CatalogPreviewResponse.PreviewItem(
                            p.getExternalProductId(),
                            p.getSku(),
                            p.getTitle(),
                            p.getCategory(),
                            p.getDescription(),
                            p.getPrice(),
                            p.getCurrency(),
                            p.getStockQuantity(),
                            p.getReorderPoint(),
                            p.isAvailable(),
                            res.imageUrl(),
                            res.isValid(),
                            res.validationErrors()
                    );
                }).toList();

        return new CatalogPreviewResponse(previewItems.size(), previewItems);
    }

    public CatalogSyncExecutionResponse triggerSync(String id) {
        String tenantId = tenantContext.tenantId();
        CatalogIntegration integration = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Integration not found"));

        syncProcessor.executeSync(integration.getId(), tenantId);

        return new CatalogSyncExecutionResponse(
                null, tenantId, id, "IN_PROGRESS", Instant.now(), null, 0, 0, 0, 0, List.of()
        );
    }

    public List<CatalogSyncExecutionResponse> executions(String id) {
        String tenantId = tenantContext.tenantId();
        return executionRepository.findByIntegrationIdAndTenantIdOrderByStartedAtDesc(id, tenantId).stream()
                .map(e -> new CatalogSyncExecutionResponse(
                        e.getId(), e.getTenantId(), e.getIntegrationId(), e.getStatus(),
                        e.getStartedAt(), e.getFinishedAt(), e.getCreatedCount(), e.getUpdatedCount(),
                        e.getSkippedCount(), e.getErrorCount(), e.getErrorSummary()
                )).toList();
    }

    private CatalogProviderConnector findConnector(String providerType) {
        return connectors.stream()
                .filter(c -> c.supports(providerType))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported provider type: " + providerType));
    }

    private CatalogIntegrationResponse toResponse(CatalogIntegration integration) {
        boolean hasCredentials = integration.getEncryptedCredentials() != null && !integration.getEncryptedCredentials().isBlank();
        return new CatalogIntegrationResponse(
                integration.getId(),
                integration.getTenantId(),
                integration.getName(),
                integration.getProviderType(),
                integration.getBaseUrl(),
                integration.getStatus(),
                hasCredentials,
                integration.getAuthType(),
                integration.getApiKeyHeaderName(),
                integration.getApiKeyQueryName(),
                integration.getSyncMode(),
                integration.getSchedule(),
                integration.getFieldMapping(),
                integration.getConflictStrategy(),
                integration.isSkuFallbackEnabled(),
                integration.getLastSyncAt(),
                integration.getLastSyncStatus(),
                integration.getCreatedAt(),
                integration.getUpdatedAt()
        );
    }
}
