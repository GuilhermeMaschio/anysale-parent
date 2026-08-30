package com.anysale.lead.adapters.in.rest.query;

import com.anysale.lead.adapters.in.rest.dto.AiSkillRequest;
import com.anysale.lead.adapters.in.rest.dto.AiSkillResponse;
import com.anysale.lead.aplication.ai.AiSkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/ai/skills")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AiSkillController {
    private final AiSkillService service;
    @GetMapping public List<AiSkillResponse> list() { return service.skills().stream().map(this::response).toList(); }
    @PutMapping("/{profile}") public AiSkillResponse update(@PathVariable String profile, @Valid @RequestBody AiSkillRequest request) { return response(service.update(profile, request.content())); }
    @DeleteMapping("/{profile}") @ResponseStatus(HttpStatus.NO_CONTENT) public void reset(@PathVariable String profile) { service.reset(profile); }
    private AiSkillResponse response(AiSkillService.SkillView skill) { return new AiSkillResponse(skill.profile(), skill.label(), skill.content(), skill.customized(), skill.updatedAt()); }
}
