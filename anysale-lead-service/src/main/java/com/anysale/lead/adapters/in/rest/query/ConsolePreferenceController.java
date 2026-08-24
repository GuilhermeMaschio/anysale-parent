package com.anysale.lead.adapters.in.rest.query;

import com.anysale.lead.adapters.in.rest.dto.ConsolePreferenceRequest;
import com.anysale.lead.adapters.in.rest.dto.ConsolePreferenceResponse;
import com.anysale.lead.aplication.ConsolePreferenceService;
import com.anysale.lead.domain.model.ConsolePreference;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/console/preferences")
@RequiredArgsConstructor
public class ConsolePreferenceController {
    private final ConsolePreferenceService service;

    @GetMapping
    public ConsolePreferenceResponse preference() { return response(service.preference()); }

    @PutMapping
    public ResponseEntity<ConsolePreferenceResponse> update(@Valid @RequestBody ConsolePreferenceRequest request) {
        return ResponseEntity.ok(response(service.updateTheme(request.colorTheme())));
    }

    private ConsolePreferenceResponse response(ConsolePreference preference) {
        return new ConsolePreferenceResponse(preference.getColorTheme(), preference.getUpdatedAt());
    }
}
