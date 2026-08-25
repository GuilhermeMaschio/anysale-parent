package com.anysale.lead.aplication.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/** Reads only sellable products from the catalog; a catalog outage never blocks local AI fallback. */
@Slf4j
@Component
public class CatalogContextService {
    private static final int MAX_PRODUCTS = 12;
    private final RestClient restClient;

    public CatalogContextService(RestClient.Builder builder,
                                 @Value("${catalog-service.base-url:http://localhost:8082}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl.replaceFirst("/+$", "")).build();
    }

    public List<CatalogProduct> availableProducts() {
        try {
            CatalogProduct[] products = restClient.get().uri("/v1/products")
                    .headers(headers -> bearerToken(headers))
                    .retrieve().body(CatalogProduct[].class);
            return products == null ? List.of() : Arrays.stream(products)
                    .filter(product -> product.available && product.availableQuantity > 0)
                    .limit(MAX_PRODUCTS).toList();
        } catch (Exception exception) {
            log.debug("Catalog context unavailable ({}); continuing without product context", exception.getClass().getSimpleName());
            return List.of();
        }
    }

    private void bearerToken(org.springframework.http.HttpHeaders headers) {
        Object authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) headers.setBearerAuth(jwt.getToken().getTokenValue());
    }

    public record CatalogProduct(String sku, String title, String category, String description, String currency,
                                 String vendor, BigDecimal price, int availableQuantity, boolean available) { }
}
