package com.anysale.catalog.adapters.out.persistence;

import com.anysale.catalog.domain.model.CatalogIntegration;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface CatalogIntegrationRepository extends MongoRepository<CatalogIntegration, String> {
    List<CatalogIntegration> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<CatalogIntegration> findByIdAndTenantId(String id, String tenantId);
}
