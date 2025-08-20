package com.anysale.gateway.adapters.in.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/v1/ingest")
public class IngestionController {
    private final WebClient web;
    public IngestionController(@Value("${lead-service.base-url}") String base){
        this.web = WebClient.builder().baseUrl(base).build();
    }

    @PostMapping("/lead")
    public Object ingest(@RequestBody Map<String,Object> p){
        Map<String,Object> req = Map.of(
                "name", p.getOrDefault("full_name",""),
                "email", p.get("email"),
                "phone", p.get("phone"),
                "source", p.get("source"),
                "desiredCategory", p.get("desired_category"),
                "desiredTags", p.getOrDefault("desired_tags", List.of())
        );
        return web.post().uri("/v1/leads")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(Object.class);
    }
}
