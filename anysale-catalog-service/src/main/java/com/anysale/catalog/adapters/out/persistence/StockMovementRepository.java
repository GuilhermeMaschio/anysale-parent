package com.anysale.catalog.adapters.out.persistence;

import com.anysale.catalog.domain.model.StockMovement;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface StockMovementRepository extends MongoRepository<StockMovement, String> {
    List<StockMovement> findByTenantIdAndProductIdOrderByCreatedAtDesc(String tenantId, String productId);
}
