package com.anysale.lead.aplication;

import com.anysale.lead.adapters.out.persistence.ConsolePreferenceJpaRepository;
import com.anysale.lead.domain.model.ConsolePreference;
import com.anysale.lead.tenant.TenantContext;
import com.anysale.lead.tenant.UserIdentityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ConsolePreferenceService {
    private final ConsolePreferenceJpaRepository repository;
    private final TenantContext tenantContext;
    private final UserIdentityContext userIdentityContext;

    @Transactional(readOnly = true)
    public ConsolePreference preference() {
        String tenantId = tenantContext.tenantId();
        String userId = userIdentityContext.userId();
        return repository.findById(new ConsolePreference.Id(tenantId, userId)).orElseGet(() -> defaults(tenantId, userId));
    }

    @Transactional
    public ConsolePreference updateTheme(String colorTheme) {
        String tenantId = tenantContext.tenantId();
        String userId = userIdentityContext.userId();
        ConsolePreference preference = repository.findById(new ConsolePreference.Id(tenantId, userId)).orElseGet(() -> defaults(tenantId, userId));
        preference.setColorTheme(colorTheme);
        preference.setUpdatedAt(Instant.now());
        return repository.save(preference);
    }

    private ConsolePreference defaults(String tenantId, String userId) {
        ConsolePreference preference = new ConsolePreference();
        preference.setTenantId(tenantId);
        preference.setUserId(userId);
        preference.setColorTheme("light");
        preference.setUpdatedAt(Instant.now());
        return preference;
    }
}
