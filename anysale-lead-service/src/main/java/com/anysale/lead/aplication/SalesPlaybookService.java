package com.anysale.lead.aplication;

import com.anysale.lead.adapters.in.rest.dto.SalesPlaybookRequest;
import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.adapters.out.persistence.SalesPlaybookJpaRepository;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.domain.model.SalesPlaybook;
import com.anysale.lead.tenant.TenantContext;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SalesPlaybookService {
    private final SalesPlaybookJpaRepository repository;
    private final LeadJpaRepository leads;
    private final TenantContext tenants;

    public SalesPlaybookService(SalesPlaybookJpaRepository repository, LeadJpaRepository leads, TenantContext tenants) {
        this.repository = repository;
        this.leads = leads;
        this.tenants = tenants;
    }

    @Transactional
    public SalesPlaybook create(SalesPlaybookRequest request) {
        String tenantId = tenants.tenantId();
        if (repository.findByTenantIdAndDefaultPlaybookTrue(tenantId).isEmpty() && !request.defaultPlaybook()) {
            throw badRequest("The first playbook must be the default");
        }
        SalesPlaybook playbook = new SalesPlaybook();
        apply(playbook, request, tenantId);
        return repository.save(playbook);
    }

    @Transactional
    public SalesPlaybook update(UUID id, SalesPlaybookRequest request) {
        SalesPlaybook playbook = scoped(id);
        apply(playbook, request, playbook.getTenantId());
        return repository.save(playbook);
    }

    @Transactional(readOnly = true)
    public List<SalesPlaybook> list() {
        return repository.findByTenantIdOrderByNameAsc(tenants.tenantId());
    }

    @Transactional(readOnly = true)
    public SalesPlaybook resolve(UUID leadId) {
        Lead lead = leads.findByIdWithTags(leadId)
                .orElseThrow(() -> notFound("Lead not found"));
        String tenantId = tenants.tenantId();
        if (!tenantId.equals(lead.getTenantId())) {
            throw notFound("Lead not found");
        }
        if (lead.getDesiredCategory() == null || lead.getDesiredCategory().isBlank()) {
            return fallback(tenantId);
        }
        return repository.findForCategory(tenantId, lead.getDesiredCategory())
                .orElseGet(() -> fallback(tenantId));
    }

    private SalesPlaybook scoped(UUID id) {
        return repository.findById(id)
                .filter(playbook -> tenants.tenantId().equals(playbook.getTenantId()))
                .orElseThrow(() -> notFound("Playbook not found"));
    }

    private SalesPlaybook fallback(String tenantId) {
        return repository.findByTenantIdAndDefaultPlaybookTrue(tenantId)
                .filter(SalesPlaybook::isActive)
                .orElseThrow(() -> notFound("Default playbook not configured"));
    }

    private void apply(SalesPlaybook playbook, SalesPlaybookRequest request, String tenantId) {
        if (request.defaultPlaybook() && !request.active()) {
            throw badRequest("The default playbook must be active");
        }
        if (!request.defaultPlaybook() && playbook.isDefaultPlaybook()) {
            throw badRequest("Assign another default playbook before removing this one");
        }

        Set<String> categories = categories(request.categories());
        ensureCategoriesAreAvailable(categories, tenantId, playbook.getId());

        if (request.defaultPlaybook()) {
            repository.findByTenantIdAndDefaultPlaybookTrue(tenantId)
                    .filter(current -> !current.getId().equals(playbook.getId()))
                    .ifPresent(current -> {
                        current.setDefaultPlaybook(false);
                        repository.saveAndFlush(current);
                    });
        }

        playbook.setTenantId(tenantId);
        playbook.setName(request.name().trim());
        playbook.setDescription(request.description());
        playbook.setActive(request.active());
        playbook.setDefaultPlaybook(request.defaultPlaybook());
        playbook.setCategories(categories);
    }

    private Set<String> categories(Set<String> requestedCategories) {
        if (requestedCategories == null) {
            return new LinkedHashSet<>();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String category : requestedCategories) {
            if (category != null && !category.isBlank()) {
                normalized.add(category.trim());
            }
        }
        return normalized;
    }

    private void ensureCategoriesAreAvailable(Set<String> categories, String tenantId, UUID playbookId) {
        for (String category : categories) {
            Optional<SalesPlaybook> assigned = repository.findAnyForCategory(tenantId, category);
            if (assigned.isPresent() && !assigned.get().getId().equals(playbookId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Category is already assigned to another playbook: " + category);
            }
        }
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    private ResponseStatusException notFound(String reason) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
    }
}
