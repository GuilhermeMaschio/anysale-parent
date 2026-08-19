package com.anysale.lead.adapters.in.rest.query;

import com.anysale.lead.adapters.in.rest.dto.AiSettingsRequest;
import com.anysale.lead.adapters.in.rest.dto.AiSettingsResponse;
import com.anysale.lead.adapters.in.rest.dto.AiUsageResponse;
import com.anysale.lead.aplication.ai.AiPolicy;
import com.anysale.lead.aplication.ai.AiSettingsService;
import com.anysale.lead.domain.model.AiSettings;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
public class AiSettingsController {
    private final AiSettingsService aiSettingsService;

    @GetMapping("/settings")
    public AiSettingsResponse settings() {
        return toResponse(aiSettingsService.settings(), aiSettingsService.policy());
    }

    @PutMapping("/settings")
    public ResponseEntity<AiSettingsResponse> update(@Valid @RequestBody AiSettingsRequest request) {
        AiSettings updated = aiSettingsService.update(request.enabled(), request.model(), request.maxOutputTokens(),
                request.monthlyRequestLimit(), request.monthlyTokenLimit());
        return ResponseEntity.ok(toResponse(updated, aiSettingsService.policy()));
    }

    @GetMapping("/usage")
    public AiUsageResponse usage() {
        AiSettingsService.UsageSummary usage = aiSettingsService.currentUsage();
        AiSettings settings = aiSettingsService.settings();
        return new AiUsageResponse(usage.month(), usage.requests(), usage.inputTokens(), usage.outputTokens(), usage.totalTokens(),
                settings.getMonthlyRequestLimit(), settings.getMonthlyTokenLimit());
    }

    private AiSettingsResponse toResponse(AiSettings settings, AiPolicy policy) {
        return new AiSettingsResponse(settings.isEnabled(), policy.providerAvailable(), policy.model(), settings.getMaxOutputTokens(),
                settings.getMonthlyRequestLimit(), settings.getMonthlyTokenLimit(), aiSettingsService.allowedModels(), settings.getUpdatedAt());
    }
}
