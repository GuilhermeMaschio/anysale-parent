package com.anysale.catalog.adapters.in.rest;

import com.anysale.catalog.adapters.in.rest.dto.ProductRequest;
import com.anysale.catalog.adapters.in.rest.dto.ProductResponse;
import com.anysale.catalog.adapters.in.rest.dto.StockMovementRequest;
import com.anysale.catalog.adapters.in.rest.dto.StockMovementResponse;
import com.anysale.catalog.application.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController @RequestMapping("/v1/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }
    @GetMapping public List<ProductResponse> list() { return service.list(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ProductResponse create(@Valid @RequestBody ProductRequest request) { return service.create(request); }
    @PutMapping("/{id}") public ProductResponse update(@PathVariable String id, @Valid @RequestBody ProductRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void archive(@PathVariable String id) { service.archive(id); }
    @PostMapping("/{id}/stock-movements") public ProductResponse move(@PathVariable String id, @Valid @RequestBody StockMovementRequest request) { return service.move(id, request); }
    @GetMapping("/{id}/stock-movements") public List<StockMovementResponse> history(@PathVariable String id) { return service.history(id); }
    @PostMapping(value = "/{id}/image", consumes = "multipart/form-data") public ProductResponse uploadImage(@PathVariable String id, @RequestParam("file") MultipartFile file) { return service.uploadImage(id, file); }
    @GetMapping("/{id}/image") public ResponseEntity<Resource> image(@PathVariable String id) {
        GridFsResource image = service.image(id);
        return ResponseEntity.ok().header("Content-Type", image.getContentType()).body(image);
    }
}
