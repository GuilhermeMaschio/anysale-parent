package com.anysale.lead.aplication.ai;

import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component("ruleBasedLeadAiAssistant")
public class RuleBasedLeadAiAssistant implements LeadAiAssistant {

    @Override
    public LeadAiDraft analyzeConversation(Lead lead, List<Interaction> interactions) {
        String latestInboundMessage = latestInboundMessage(interactions, lead.getLastMessage());
        String corpus = buildCorpus(interactions, lead.getLastMessage());
        String normalizedCorpus = corpus.toLowerCase(Locale.ROOT);

        List<String> desiredTags = inferTags(normalizedCorpus);
        String desiredCategory = inferCategory(normalizedCorpus, desiredTags);
        String intent = inferIntent(normalizedCorpus);
        Integer score = inferScore(normalizedCorpus, interactions);
        String summary = buildSummary(lead, latestInboundMessage, intent, desiredCategory);
        String nextAction = buildNextAction(intent, desiredCategory, score);
        String suggestedReply = buildSuggestedReply(lead, desiredCategory, intent);

        return new LeadAiDraft(
                summary,
                intent,
                desiredCategory,
                desiredTags,
                score,
                nextAction,
                suggestedReply
        );
    }

    private String latestInboundMessage(List<Interaction> interactions, String fallback) {
        if (interactions == null || interactions.isEmpty()) {
            return fallback;
        }

        for (int index = interactions.size() - 1; index >= 0; index--) {
            Interaction interaction = interactions.get(index);
            if (interaction != null
                    && "IN".equalsIgnoreCase(interaction.getDirection())
                    && hasText(interaction.getMessage())) {
                return interaction.getMessage().trim();
            }
        }

        return fallback;
    }

    private String buildCorpus(List<Interaction> interactions, String fallback) {
        if (interactions == null || interactions.isEmpty()) {
            return fallback == null ? "" : fallback;
        }

        return interactions.stream()
                .map(Interaction::getMessage)
                .filter(this::hasText)
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }

    private List<String> inferTags(String corpus) {
        Set<String> tags = new LinkedHashSet<>();

        addIfContains(corpus, tags, "cadeira", "cadeira");
        addIfContains(corpus, tags, "ergonom", "ergonomica");
        addIfContains(corpus, tags, "mesa", "mesa");
        addIfContains(corpus, tags, "home office", "home-office");
        addIfContains(corpus, tags, "home-office", "home-office");
        addIfContains(corpus, tags, "catalogo", "catalogo");
        addIfContains(corpus, tags, "catálogo", "catalogo");
        addIfContains(corpus, tags, "preço", "preco");
        addIfContains(corpus, tags, "preco", "preco");
        addIfContains(corpus, tags, "orçamento", "orcamento");
        addIfContains(corpus, tags, "orcamento", "orcamento");

        return new ArrayList<>(tags);
    }

    private String inferCategory(String corpus, List<String> tags) {
        if (containsAny(corpus, "cadeira", "mesa", "home office", "home-office", "ergonom")) {
            return "home-office";
        }
        if (tags.contains("catalogo")) {
            return "catalog-request";
        }
        return null;
    }

    private String inferIntent(String corpus) {
        if (containsAny(corpus, "comprar", "quero", "gostaria", "fechar", "pedido")) {
            return "BUYING";
        }
        if (containsAny(corpus, "preço", "preco", "valor", "orçamento", "orcamento")) {
            return "PRICE_CHECK";
        }
        if (containsAny(corpus, "catálogo", "catalogo", "opções", "opcoes")) {
            return "CATALOG_REQUEST";
        }
        return "DISCOVERY";
    }

    private Integer inferScore(String corpus, List<Interaction> interactions) {
        int score = 45;

        if (containsAny(corpus, "comprar", "quero", "gostaria", "fechar")) {
            score += 20;
        }
        if (containsAny(corpus, "preço", "preco", "orçamento", "orcamento", "valor")) {
            score += 15;
        }
        if (containsAny(corpus, "catálogo", "catalogo", "detalhes", "saber mais")) {
            score += 10;
        }

        int interactionBonus = interactions == null ? 0 : Math.min(10, interactions.size() * 3);
        score += interactionBonus;

        return Math.max(0, Math.min(100, score));
    }

    private String buildSummary(Lead lead, String latestInboundMessage, String intent, String desiredCategory) {
        String leadName = hasText(lead.getName()) ? lead.getName().trim() : "Contato";
        String categoryPart = desiredCategory == null ? "sem categoria inferida" : "interesse em " + desiredCategory;
        String messagePart = hasText(latestInboundMessage)
                ? "Última mensagem: \"" + latestInboundMessage.trim() + "\"."
                : "Ainda sem mensagem registrada.";

        return leadName + " entrou em contato via " + safeValue(lead.getSource(), "canal não identificado")
                + " com intenção " + intent + " e " + categoryPart + ". " + messagePart;
    }

    private String buildNextAction(String intent, String desiredCategory, Integer score) {
        if ("BUYING".equals(intent) || score >= 80) {
            return "Responder no WhatsApp com proposta objetiva e oferecer atendimento humano imediato.";
        }
        if ("PRICE_CHECK".equals(intent)) {
            return "Enviar faixa de preço e confirmar orçamento disponível antes de sugerir produtos.";
        }
        if ("CATALOG_REQUEST".equals(intent) || "home-office".equals(desiredCategory)) {
            return "Enviar catálogo curado com opções de home office e perguntar prioridade de compra.";
        }
        return "Responder com abordagem consultiva, entender contexto e oferecer ajuda personalizada.";
    }

    private String buildSuggestedReply(Lead lead, String desiredCategory, String intent) {
        String firstName = firstName(lead.getName());

        if ("BUYING".equals(intent)) {
            return "Oi, " + firstName + "! Posso te ajudar com opções de "
                    + categoryLabel(desiredCategory)
                    + ". Se quiser, já te mando as melhores sugestões e faixa de preço.";
        }
        if ("PRICE_CHECK".equals(intent)) {
            return "Oi, " + firstName + "! Consigo te passar uma faixa de preço e separar opções de "
                    + categoryLabel(desiredCategory)
                    + ". Se quiser, eu já te envio algumas sugestões.";
        }
        if ("CATALOG_REQUEST".equals(intent)) {
            return "Oi, " + firstName + "! Posso te enviar um catálogo com opções de "
                    + categoryLabel(desiredCategory)
                    + " e destacar as mais indicadas para o seu caso.";
        }
        return "Oi, " + firstName + "! Obrigado pela mensagem. Posso te ajudar com opções de "
                + categoryLabel(desiredCategory)
                + " e entender melhor o que você procura.";
    }

    private String firstName(String name) {
        if (!hasText(name)) {
            return "tudo bem";
        }
        String[] parts = name.trim().split("\\s+");
        return parts.length == 0 ? "tudo bem" : parts[0];
    }

    private String categoryLabel(String desiredCategory) {
        if (!hasText(desiredCategory) || "catalog-request".equals(desiredCategory)) {
            return "produtos";
        }
        if ("home-office".equals(desiredCategory)) {
            return "home office";
        }
        return desiredCategory;
    }

    private void addIfContains(String corpus, Set<String> tags, String keyword, String tag) {
        if (corpus.contains(keyword)) {
            tags.add(tag);
        }
    }

    private boolean containsAny(String value, String... keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeValue(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }
}
