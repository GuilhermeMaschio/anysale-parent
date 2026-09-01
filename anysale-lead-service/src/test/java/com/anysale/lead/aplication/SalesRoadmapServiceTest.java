package com.anysale.lead.aplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.tenant.TenantContext;
import com.anysale.lead.tenant.UserIdentityContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SalesRoadmapServiceTest {
    @Mock LeadJpaRepository leads;
    @Mock TenantContext tenants;
    @Mock UserIdentityContext users;
    @InjectMocks SalesRoadmapService service;

    @Test
    void marksTheLeadAssigneeAsThePrimaryRelationship() {
        Lead owned = lead("agent-1");
        Lead worked = lead("agent-2");
        when(tenants.tenantId()).thenReturn("tenant-a");
        when(users.userId()).thenReturn("agent-1");
        when(leads.findRoadmapPortfolio("tenant-a", "agent-1")).thenReturn(List.of(owned, worked));

        List<SalesRoadmapService.PortfolioLead> portfolio = service.portfolio();

        assertThat(portfolio).extracting(SalesRoadmapService.PortfolioLead::relationship)
                .containsExactly(SalesRoadmapService.Relationship.RESPONSIBLE,
                        SalesRoadmapService.Relationship.TASK_ACTIVITY);
    }

    @Test
    void preventsOpeningARoadmapOutsideTheCurrentPortfolio() {
        when(tenants.tenantId()).thenReturn("tenant-a");
        when(users.userId()).thenReturn("agent-1");
        when(leads.findRoadmapPortfolio("tenant-a", "agent-1")).thenReturn(List.of());

        assertThatThrownBy(() -> service.assertCurrentUserCanFollow(UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not part of the current user's roadmap");
    }

    private Lead lead(String assignedTo) {
        Lead lead = new Lead();
        lead.setId(UUID.randomUUID());
        lead.setAssignedTo(assignedTo);
        return lead;
    }
}
