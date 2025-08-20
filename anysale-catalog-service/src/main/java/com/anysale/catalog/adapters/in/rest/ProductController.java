package com.anysale.catalog.adapters.in.rest;

import com.anysale.catalog.adapters.out.persistence.ProductRepository;
import com.anysale.catalog.adapters.out.persistence.ProductRepository;
import com.anysale.catalog.domain.model.Product;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/v1/products")
public class ProductController {
    private final ProductRepository repo;
    public ProductController(ProductRepository repo){ this.repo = repo; }

    @PostMapping public List<Product> seed(@RequestBody List<Product> body){ return repo.saveAll(body); }
    @GetMapping public List<Product> list(){ return repo.findAll(); }
}
