package com.anysale.catalog.application;

import com.anysale.catalog.adapters.in.rest.dto.*;
import com.anysale.catalog.adapters.out.persistence.ProductRepository;
import com.anysale.catalog.adapters.out.persistence.StockMovementRepository;
import com.anysale.catalog.domain.model.Product;
import com.anysale.catalog.domain.model.StockMovement;
import com.anysale.catalog.tenant.TenantContext;
import com.anysale.catalog.tenant.UserIdentityContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.web.multipart.MultipartFile;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository products; private final StockMovementRepository movements;
    private final TenantContext tenantContext; private final UserIdentityContext userIdentity; private final GridFsTemplate gridFs;
    public ProductService(ProductRepository products, StockMovementRepository movements, TenantContext tenantContext, UserIdentityContext userIdentity, GridFsTemplate gridFs) {
        this.products = products; this.movements = movements; this.tenantContext = tenantContext; this.userIdentity = userIdentity; this.gridFs = gridFs;
    }
    public List<ProductResponse> list() { String tenant = tenantContext.tenantId(); return products.findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(tenant).stream().map(this::response).toList(); }
    public ProductResponse create(ProductRequest request) {
        String tenant = tenantContext.tenantId(); validateSku(tenant, request.sku(), null);
        Product product = new Product(); product.setTenantId(tenant); product.setStockQuantity(request.initialStock()); product.setReservedQuantity(0); product.setCreatedAt(Instant.now());
        apply(product, request); Product saved = products.save(product);
        if (request.initialStock() > 0) saveMovement(saved, "IN", request.initialStock(), "Estoque inicial");
        return response(saved);
    }
    public ProductResponse update(String id, ProductRequest request) {
        String tenant = tenantContext.tenantId(); Product product = product(id, tenant); validateSku(tenant, request.sku(), id); apply(product, request); product.setUpdatedAt(Instant.now()); return response(products.save(product));
    }
    public ProductResponse move(String id, StockMovementRequest request) {
        String tenant = tenantContext.tenantId(); Product product = product(id, tenant); int delta = "OUT".equals(request.type()) ? -request.quantity() : request.quantity();
        if (product.getStockQuantity() + delta < product.getReservedQuantity()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for this movement");
        product.setStockQuantity(product.getStockQuantity() + delta); product.setUpdatedAt(Instant.now()); Product saved = products.save(product); saveMovement(saved, request.type(), request.quantity(), request.reason()); return response(saved);
    }
    public List<StockMovementResponse> history(String id) { String tenant = tenantContext.tenantId(); product(id, tenant); return movements.findByTenantIdAndProductIdOrderByCreatedAtDesc(tenant, id).stream().map(m -> new StockMovementResponse(m.getId(), m.getType(), m.getQuantity(), m.getBalanceAfter(), m.getReason(), m.getCreatedBy(), m.getCreatedAt())).toList(); }
    public void archive(String id) { String tenant = tenantContext.tenantId(); Product product = product(id, tenant); product.setAvailable(false); product.setDeletedAt(Instant.now()); product.setUpdatedAt(Instant.now()); products.save(product); }
    public ProductResponse uploadImage(String id, MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024 || file.getContentType() == null || !file.getContentType().startsWith("image/")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use an image up to 5 MB");
        String tenant = tenantContext.tenantId(); Product product = product(id, tenant);
        try {
            if (product.getImageId() != null) gridFs.delete(query(where("_id").is(product.getImageId())));
            String imageId = gridFs.store(file.getInputStream(), product.getId() + "-" + file.getOriginalFilename(), file.getContentType(), java.util.Map.of("tenantId", tenant, "productId", product.getId())).toString();
            product.setImageId(imageId); product.setImageContentType(file.getContentType()); product.setUpdatedAt(Instant.now()); return response(products.save(product));
        } catch (java.io.IOException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read product image"); }
    }
    public GridFsResource image(String id) { String tenant = tenantContext.tenantId(); Product product = product(id, tenant); if (product.getImageId() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product image not found"); return gridFs.getResource(gridFs.findOne(query(where("_id").is(product.getImageId())))); }
    private Product product(String id, String tenant) { return products.findByIdAndTenantIdAndDeletedAtIsNull(id, tenant).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")); }
    private void validateSku(String tenant, String sku, String id) { boolean exists = id == null ? products.existsByTenantIdAndSkuAndDeletedAtIsNull(tenant, sku) : products.existsByTenantIdAndSkuAndIdNotAndDeletedAtIsNull(tenant, sku, id); if (exists) throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists"); }
    private void apply(Product p, ProductRequest r) { p.setSku(r.sku().trim()); p.setTitle(r.title().trim()); p.setCategory(r.category().trim()); p.setDescription(r.description()); p.setCurrency(r.currency() == null || r.currency().isBlank() ? "BRL" : r.currency().toUpperCase()); p.setVendor(r.vendor()); p.setPrice(r.price()); p.setTags(r.tags() == null ? List.of() : r.tags()); p.setAvailable(r.available()); p.setReorderPoint(r.reorderPoint()); p.setUpdatedAt(Instant.now()); }
    private void saveMovement(Product p, String type, int quantity, String reason) { StockMovement m = new StockMovement(); m.setTenantId(p.getTenantId()); m.setProductId(p.getId()); m.setType(type); m.setQuantity(quantity); m.setBalanceAfter(p.getStockQuantity()); m.setReason(reason); m.setCreatedBy(userIdentity.userId()); m.setCreatedAt(Instant.now()); movements.save(m); }
    private ProductResponse response(Product p) { int availableQuantity = Math.max(0, p.getStockQuantity() - p.getReservedQuantity()); return new ProductResponse(p.getId(), p.getSku(), p.getTitle(), p.getCategory(), p.getDescription(), p.getCurrency(), p.getVendor(), p.getPrice(), p.getTags(), p.isAvailable(), p.getStockQuantity(), p.getReservedQuantity(), availableQuantity, p.getReorderPoint(), availableQuantity <= p.getReorderPoint(), p.getImageId() != null, p.getUpdatedAt()); }
}
