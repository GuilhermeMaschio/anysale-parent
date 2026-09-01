package com.anysale.lead.aplication;

import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.tenant.TenantContext;
import com.anysale.lead.tenant.UserIdentityContext;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Builds the seller workspace without making a task assignment a second lead ownership model.
 * The lead assignee remains the primary relationship; task activity only extends the user's
 * visible portfolio when they have already worked that lead.
 */
@Service
public class SalesRoadmapService {
    public enum Relationship { RESPONSIBLE, TASK_ACTIVITY }

    public record PortfolioLead(Lead lead, Relationship relationship) { }

    private final LeadJpaRepository leads;
    private final TenantContext tenants;
    private final UserIdentityContext users;

    public SalesRoadmapService(LeadJpaRepository leads, TenantContext tenants, UserIdentityContext users) {
        this.leads = leads;
        this.tenants = tenants;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<PortfolioLead> portfolio() {
        String tenant = tenants.tenantId();
        String user = users.userId();
        return leads.findRoadmapPortfolio(tenant, user).stream()
                .map(lead -> new PortfolioLead(lead, relationship(lead, user)))
                .toList();
    }

    @Transactional(readOnly = true)
    public void assertCurrentUserCanFollow(UUID leadId) {
        String tenant = tenants.tenantId();
        String user = users.userId();
        boolean belongsToPortfolio = leads.findRoadmapPortfolio(tenant, user).stream()
                .anyMatch(lead -> lead.getId().equals(leadId));
        if (!belongsToPortfolio) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead is not part of the current user's roadmap");
        }
    }

    private Relationship relationship(Lead lead, String userId) {
        return userId.equals(lead.getAssignedTo()) ? Relationship.RESPONSIBLE : Relationship.TASK_ACTIVITY;
    }
}
