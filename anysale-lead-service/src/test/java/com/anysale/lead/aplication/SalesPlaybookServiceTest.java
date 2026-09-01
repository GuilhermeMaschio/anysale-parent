package com.anysale.lead.aplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.anysale.lead.adapters.in.rest.dto.SalesPlaybookRequest;
import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.adapters.out.persistence.SalesPlaybookJpaRepository;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.domain.model.SalesPlaybook;
import com.anysale.lead.tenant.TenantContext;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SalesPlaybookServiceTest {
    @Mock SalesPlaybookJpaRepository repository;
    @Mock LeadJpaRepository leads;
    @Mock TenantContext tenants;
    @InjectMocks SalesPlaybookService service;

    private static final String TENANT = "tenant-a";

    @BeforeEach
    void setUp() {
        when(tenants.tenantId()).thenReturn(TENANT);
    }

    @Test
    void rejectsFirstPlaybookWhenItIsNotDefault() {
        when(repository.findByTenantIdAndDefaultPlaybookTrue(TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(false, true, Set.of())))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsCategoryAssignedToAnotherPlaybook() {
        when(repository.findByTenantIdAndDefaultPlaybookTrue(TENANT)).thenReturn(Optional.of(playbook(UUID.randomUUID())));
        when(repository.findAnyForCategory(TENANT, "consultoria"))
                .thenReturn(Optional.of(playbook(UUID.randomUUID())));

        assertThatThrownBy(() -> service.create(request(false, true, Set.of("consultoria"))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void resolvesCategorySpecificPlaybookBeforeDefault() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead();
        lead.setTenantId(TENANT);
        lead.setDesiredCategory("consultoria");
        SalesPlaybook specific = playbook(UUID.randomUUID());

        when(leads.findByIdWithTags(leadId)).thenReturn(Optional.of(lead));
        when(repository.findForCategory(TENANT, "consultoria")).thenReturn(Optional.of(specific));

        assertThat(service.resolve(leadId)).isSameAs(specific);
    }

    @Test
    void fallsBackToActiveDefaultWhenNoCategoryMatches() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead();
        lead.setTenantId(TENANT);
        lead.setDesiredCategory("produto-sem-playbook");
        SalesPlaybook defaultPlaybook = playbook(UUID.randomUUID());

        when(leads.findByIdWithTags(leadId)).thenReturn(Optional.of(lead));
        when(repository.findForCategory(TENANT, "produto-sem-playbook")).thenReturn(Optional.empty());
        when(repository.findByTenantIdAndDefaultPlaybookTrue(TENANT)).thenReturn(Optional.of(defaultPlaybook));

        assertThat(service.resolve(leadId)).isSameAs(defaultPlaybook);
    }

    private SalesPlaybook playbook(UUID id) {
        SalesPlaybook playbook = new SalesPlaybook();
        setId(playbook, id);
        playbook.setActive(true);
        return playbook;
    }

    private SalesPlaybookRequest request(boolean defaultPlaybook, boolean active, Set<String> categories) {
        return new SalesPlaybookRequest("Playbook comercial", null, active, defaultPlaybook, categories);
    }

    private void setId(SalesPlaybook playbook, UUID id) {
        try {
            Field field = SalesPlaybook.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(playbook, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
