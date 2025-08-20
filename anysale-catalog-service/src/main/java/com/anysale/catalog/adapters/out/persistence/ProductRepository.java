package com.anysale.catalog.adapters.out.persistence;

import com.anysale.catalog.domain.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByCategory(String category);
}
