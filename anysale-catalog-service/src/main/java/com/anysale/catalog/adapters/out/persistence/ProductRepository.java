package com.anysale.catalog.adapters.out.persistence;

import com.anysale.catalog.domain.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(String tenantId);
    List<Product> findByTenantIdAndCategoryAndAvailableTrueAndDeletedAtIsNull(String tenantId, String category);
    java.util.Optional<Product> findByIdAndTenantIdAndDeletedAtIsNull(String id, String tenantId);
    java.util.Optional<Product> findByTenantIdAndSourceAndExternalProductIdAndDeletedAtIsNull(String tenantId, String source, String externalProductId);
    java.util.Optional<Product> findByTenantIdAndSkuAndDeletedAtIsNull(String tenantId, String sku);
    boolean existsByTenantIdAndSkuAndDeletedAtIsNull(String tenantId, String sku);
    boolean existsByTenantIdAndSkuAndIdNotAndDeletedAtIsNull(String tenantId, String sku, String id);
}

