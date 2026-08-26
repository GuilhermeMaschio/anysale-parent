package com.anysale.catalog.adapters.in.rest;

import com.anysale.catalog.adapters.in.rest.dto.*;
import com.anysale.catalog.application.CatalogIntegrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/catalog-integrations")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogIntegrationController {

    private final CatalogIntegrationService service;

    public CatalogIntegrationController(CatalogIntegrationService service) {
        this.service = service;
    }

    @GetMapping
    public List<CatalogIntegrationResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogIntegrationResponse create(@Valid @RequestBody CatalogIntegrationRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public CatalogIntegrationResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public CatalogIntegrationResponse update(@PathVariable String id, @Valid @RequestBody CatalogIntegrationRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    @PostMapping("/{id}/test")
    public TestConnectionResponse testConnection(@PathVariable String id) {
        return service.testConnection(id);
    }

    @PostMapping("/{id}/preview")
    public CatalogPreviewResponse preview(@PathVariable String id) {
        return service.preview(id);
    }

    @PostMapping("/{id}/sync")
    public CatalogSyncExecutionResponse triggerSync(@PathVariable String id) {
        return service.triggerSync(id);
    }

    @GetMapping("/{id}/executions")
    public List<CatalogSyncExecutionResponse> executions(@PathVariable String id) {
        return service.executions(id);
    }
}
