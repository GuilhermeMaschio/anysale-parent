package com.anysale.catalog.adapters.in.messaging;

import com.anysale.catalog.adapters.out.persistence.ProductRepository;
import com.anysale.catalog.adapters.out.http.LeadClient;
import com.anysale.catalog.domain.model.Product;
import com.anysale.contracts.event.LeadCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class LeadCreatedListener {
    private final ProductRepository repo; private final LeadClient leadClient;
    public LeadCreatedListener(ProductRepository r, LeadClient c){ this.repo=r; this.leadClient=c; }

    @KafkaListener(topics = "lead.created", groupId = "catalog-service")
    public void on(LeadCreatedEvent evt){
        List<Product> candidates = repo.findByCategory(evt.getDesiredCategory());
        List<String> tags = evt.getDesiredTags() != null ? evt.getDesiredTags() : List.of();

        List<Map<String,Object>> top3 = candidates.stream()
                .map(p -> Map.entry(p, score(p, tags)))
                .sorted(Map.Entry.<Product,Integer>comparingByValue().reversed())
                .limit(3)
                .map(e -> Map.<String,Object>of(
                        "productId", e.getKey().getId(),
                        "title", e.getKey().getTitle(),
                        "price", e.getKey().getPrice() != null ? e.getKey().getPrice() : BigDecimal.ZERO,
                        "currency", e.getKey().getCurrency(),
                        "vendor", e.getKey().getVendor()
                ))
                .collect(Collectors.toList());

        Map<String,Object> body = Map.of("suggestions", top3);
        leadClient.patchSuggestions(evt.getId().toString(), body);
    }

    private int score(Product p, List<String> desired){
        int s = 0;
        if (p.isAvailable()) s += 2;
        if (p.getTags() != null) s += (int) p.getTags().stream().filter(desired::contains).count();
        s += 5; // mesma categoria (já filtrada)
        return s;
    }
}
