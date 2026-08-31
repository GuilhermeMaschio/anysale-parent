package com.anysale.lead.adapters.in.rest.query;

import com.anysale.lead.adapters.in.rest.dto.ManagedUserCreateRequest;
import com.anysale.lead.adapters.in.rest.dto.ManagedUserResponse;
import com.anysale.lead.adapters.in.rest.dto.ManagedUserUpdateRequest;
import com.anysale.lead.aplication.KeycloakUserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {
    private final KeycloakUserManagementService service;

    @GetMapping
    public List<ManagedUserResponse> list(@RequestParam(required = false) String search) { return service.list(search); }

    @PostMapping
    public ResponseEntity<ManagedUserResponse> create(@Valid @RequestBody ManagedUserCreateRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ManagedUserResponse update(@PathVariable String id, @Valid @RequestBody ManagedUserUpdateRequest request) {
        return service.update(id, request);
    }
}
