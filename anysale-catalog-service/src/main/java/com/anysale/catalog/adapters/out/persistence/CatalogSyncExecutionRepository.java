package com.anysale.catalog.adapters.out.persistence;

import com.anysale.catalog.domain.model.CatalogSyncExecution;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CatalogSyncExecutionRepository extends MongoRepository<CatalogSyncExecution, String> {
    List<CatalogSyncExecution> findByIntegrationIdAndTenantIdOrderByStartedAtDesc(String integrationId, String tenantId);
}
