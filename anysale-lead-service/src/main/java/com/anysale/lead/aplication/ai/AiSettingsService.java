package com.anysale.lead.aplication.ai;

import com.anysale.lead.adapters.out.persistence.AiSettingsJpaRepository;
import com.anysale.lead.adapters.out.persistence.AiUsageJpaRepository;
import com.anysale.lead.domain.model.AiSettings;
import com.anysale.lead.domain.model.AiUsage;
import com.anysale.lead.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiSettingsService {
    private final AiSettingsJpaRepository settingsRepository;
    private final AiUsageJpaRepository usageRepository;
    private final TenantContext tenantContext;

    @Value("${anysale.ai.openai.enabled:false}")
    private boolean providerEnabled;
    @Value("${anysale.ai.openai.model:}")
    private String defaultModel;
    @Value("${anysale.ai.openai.allowed-models:}")
    private String allowedModelsValue;

    @Transactional(readOnly = true)
    public AiPolicy policy() {
        AiSettings settings = settings();
        UsageSummary usage = currentUsage();
        String model = hasText(settings.getModel()) ? settings.getModel() : defaultModel;
        return new AiPolicy(providerEnabled, providerEnabled && settings.isEnabled(), model, settings.getMaxOutputTokens(),
                settings.getMonthlyRequestLimit(), settings.getMonthlyTokenLimit(), usage.requests(), usage.totalTokens());
    }

    @Transactional(readOnly = true)
    public AiSettings settings() {
        return settingsRepository.findById(tenantContext.tenantId()).orElseGet(this::defaultSettings);
    }

    @Transactional
    public AiSettings update(boolean enabled, String model, int maxOutputTokens, Integer monthlyRequestLimit, Long monthlyTokenLimit) {
        if (maxOutputTokens < 100 || maxOutputTokens > 4_000) {
            throw new IllegalArgumentException("maxOutputTokens must be between 100 and 4000");
        }
        if (monthlyRequestLimit != null && monthlyRequestLimit < 1 || monthlyTokenLimit != null && monthlyTokenLimit < 1) {
            throw new IllegalArgumentException("monthly limits must be positive when configured");
        }
        if (enabled && !hasText(model)) {
            throw new IllegalArgumentException("A model is required when AI is enabled");
        }
        if (hasText(model) && !allowedModels().isEmpty() && !allowedModels().contains(model)) {
            throw new IllegalArgumentException("The selected model is not allowed by this environment");
        }

        AiSettings settings = settingsRepository.findById(tenantContext.tenantId()).orElseGet(this::defaultSettings);
        settings.setEnabled(enabled);
        settings.setModel(blankToNull(model));
        settings.setMaxOutputTokens(maxOutputTokens);
        settings.setMonthlyRequestLimit(monthlyRequestLimit);
        settings.setMonthlyTokenLimit(monthlyTokenLimit);
        settings.setUpdatedAt(Instant.now());
        return settingsRepository.save(settings);
    }

    @Transactional
    public void recordUsage(String model, int inputTokens, int outputTokens) {
        AiUsage usage = new AiUsage();
        usage.setTenantId(tenantContext.tenantId());
        usage.setProvider("OPENAI");
        usage.setModel(model);
        usage.setInputTokens(Math.max(0, inputTokens));
        usage.setOutputTokens(Math.max(0, outputTokens));
        usage.setTotalTokens(Math.max(0, inputTokens) + Math.max(0, outputTokens));
        usageRepository.save(usage);
    }

    @Transactional(readOnly = true)
    public UsageSummary currentUsage() {
        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        Instant from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant until = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Object[] values = usageRepository.summarize(tenantContext.tenantId(), from, until).stream().findFirst().orElse(new Object[4]);
        return new UsageSummary(number(values, 0), number(values, 1), number(values, 2), number(values, 3), month.toString());
    }

    public List<String> allowedModels() {
        return Arrays.stream(allowedModelsValue.split(","))
                .map(String::trim).filter(this::hasText).distinct().toList();
    }

    private AiSettings defaultSettings() {
        AiSettings settings = new AiSettings();
        settings.setTenantId(tenantContext.tenantId());
        settings.setEnabled(false);
        settings.setModel(blankToNull(defaultModel));
        settings.setMaxOutputTokens(700);
        return settings;
    }

    private long number(Object[] values, int index) {
        return values.length > index && values[index] instanceof Number number ? number.longValue() : 0L;
    }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String blankToNull(String value) { return hasText(value) ? value.trim() : null; }

    public record UsageSummary(long requests, long inputTokens, long outputTokens, long totalTokens, String month) { }
}
