package com.anysale.lead.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConsoleCorsConfig implements WebMvcConfigurer {
    private final String consoleOrigin;
    public ConsoleCorsConfig(@Value("${anysale.console.origin:http://localhost:5173}") String consoleOrigin) { this.consoleOrigin = consoleOrigin; }
    @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/v1/**").allowedOrigins(consoleOrigin).allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS").allowedHeaders("Authorization", "Content-Type", "X-Internal-Token", "Idempotency-Key");
    }
}
