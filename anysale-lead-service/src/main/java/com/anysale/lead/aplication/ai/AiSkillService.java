package com.anysale.lead.aplication.ai;

import com.anysale.lead.domain.model.AiSettings;
import com.anysale.lead.domain.model.AiSkillOverride;
import com.anysale.lead.adapters.out.persistence.AiSkillOverrideJpaRepository;
import com.anysale.lead.tenant.TenantContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.time.Instant;
import java.util.List;

/** Loads versioned, product-owned conversation skills; tenant settings can refine but never replace them. */
@Service
public class AiSkillService {
    private static final Map<String, String> SKILLS = Map.of(
            "CONSULTATIVE", "ai-skills/atendimento-consultivo-v1.md",
            "DIRECT", "ai-skills/atendimento-direto-v1.md",
            "PREMIUM", "ai-skills/atendimento-premium-v1.md",
            "REACTIVATION", "ai-skills/reativacao-v1.md"
    );
    private static final Map<String, String> LABELS = Map.of("CONSULTATIVE", "Consultivo", "DIRECT", "Direto", "PREMIUM", "Premium", "REACTIVATION", "Reativação");
    private final AiSkillOverrideJpaRepository overrideRepository;
    private final TenantContext tenantContext;

    public AiSkillService(AiSkillOverrideJpaRepository overrideRepository, TenantContext tenantContext) {
        this.overrideRepository = overrideRepository; this.tenantContext = tenantContext;
    }

    public String instructions(AiSettings settings) {
        String profile = normalizedProfile(settings.getServiceProfile());
        String skill = overrideRepository.findByTenantIdAndProfile(tenantContext.tenantId(), profile)
                .map(AiSkillOverride::getContent).orElseGet(() -> baseSkill(profile));
        return skill + """

                ## Configuração desta empresa
                Tom: %s
                Formalidade: %s
                Tamanho preferido da resposta: %s
                Postura comercial: %s

                ## Instruções adicionais da empresa
                %s

                ## Exemplos aprovados
                Use somente como referência de estilo; nunca copie fatos, nomes, preços ou promessas.
                %s

                ## Exemplos a evitar
                Não replique este tom, estrutura ou abordagem.
                %s
                """.formatted(value(settings.getTone(), "WARM"), value(settings.getFormality(), "BALANCED"),
                value(settings.getResponseLength(), "CONCISE"), value(settings.getCommercialApproach(), "DISCOVER_FIRST"),
                truncate(value(settings.getCustomInstructions(), "Nenhuma."), 3000),
                truncate(value(settings.getApprovedExamples(), "Nenhum."), 4000),
                truncate(value(settings.getRejectedExamples(), "Nenhum."), 4000));
    }

    public List<SkillView> skills() {
        String tenant = tenantContext.tenantId();
        return SKILLS.keySet().stream().sorted().map(profile -> overrideRepository.findByTenantIdAndProfile(tenant, profile)
                .map(override -> new SkillView(profile, LABELS.get(profile), override.getContent(), true, override.getUpdatedAt()))
                .orElseGet(() -> new SkillView(profile, LABELS.get(profile), baseSkill(profile), false, null))).toList();
    }

    public SkillView update(String profile, String content) {
        String normalized = requireProfile(profile); String tenant = tenantContext.tenantId();
        AiSkillOverride override = overrideRepository.findByTenantIdAndProfile(tenant, normalized).orElseGet(AiSkillOverride::new);
        override.setTenantId(tenant); override.setProfile(normalized); override.setContent(content.trim()); override.setUpdatedAt(Instant.now());
        AiSkillOverride saved = overrideRepository.save(override);
        return new SkillView(normalized, LABELS.get(normalized), saved.getContent(), true, saved.getUpdatedAt());
    }

    public void reset(String profile) {
        overrideRepository.findByTenantIdAndProfile(tenantContext.tenantId(), requireProfile(profile)).ifPresent(overrideRepository::delete);
    }

    private String load(String path) {
        try (var input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Configured AI skill is unavailable", exception);
        }
    }
    private String normalizedProfile(String profile) { return SKILLS.containsKey(profile) ? profile : "CONSULTATIVE"; }
    private String requireProfile(String profile) { if (!SKILLS.containsKey(profile)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown AI skill profile"); return profile; }
    private String baseSkill(String profile) { return load(SKILLS.get(profile)); }
    private String value(String input, String fallback) { return input == null || input.isBlank() ? fallback : input.trim(); }
    private String truncate(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
    public record SkillView(String profile, String label, String content, boolean customized, Instant updatedAt) { }
}
