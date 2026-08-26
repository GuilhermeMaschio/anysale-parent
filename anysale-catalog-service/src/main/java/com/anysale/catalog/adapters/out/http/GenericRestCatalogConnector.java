package com.anysale.catalog.adapters.out.http;

import com.anysale.catalog.application.connector.CatalogProviderConnector;
import com.anysale.catalog.application.connector.ConnectionTestResult;
import com.anysale.catalog.domain.model.CatalogIntegration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Component
public class GenericRestCatalogConnector implements CatalogProviderConnector {
    private static final Logger log = LoggerFactory.getLogger(GenericRestCatalogConnector.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GenericRestCatalogConnector(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String providerType) {
        return "GENERIC_REST".equalsIgnoreCase(providerType) || providerType == null;
    }

    @Override
    public ConnectionTestResult testConnection(CatalogIntegration integration, String decryptedSecret) {
        try {
            URI uri = buildUri(integration.getBaseUrl(), 1, 1, decryptedSecret, integration);
            HttpRequest request = buildRequest(uri, integration, decryptedSecret);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return new ConnectionTestResult(true, response.statusCode(), "Connection successful (HTTP " + response.statusCode() + ")");
            } else {
                return new ConnectionTestResult(false, response.statusCode(), "Connection failed with status HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            log.error("Failed to test connection for integration {}", integration.getId(), e);
            return new ConnectionTestResult(false, 500, "Connection error: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> fetchRawProducts(CatalogIntegration integration, String decryptedSecret, int page, int pageSize) {
        try {
            URI uri = buildUri(integration.getBaseUrl(), page, pageSize, decryptedSecret, integration);
            HttpRequest request = buildRequest(uri, integration, decryptedSecret);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + response.statusCode() + " when fetching products from " + uri);
            }

            String body = response.body();
            return parseResponse(body);
        } catch (Exception e) {
            log.error("Error fetching raw products from integration {}", integration.getId(), e);
            throw new RuntimeException("Failed to fetch products from catalog provider: " + e.getMessage(), e);
        }
    }

    private URI buildUri(String baseUrl, int page, int pageSize, String secret, CatalogIntegration integration) {
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        boolean hasQuery = baseUrl.contains("?");

        if ("API_KEY_QUERY".equalsIgnoreCase(integration.getAuthType()) && secret != null && !secret.isBlank()) {
            String paramName = (integration.getApiKeyQueryName() != null && !integration.getApiKeyQueryName().isBlank())
                    ? integration.getApiKeyQueryName() : "api_key";
            urlBuilder.append(hasQuery ? "&" : "?").append(paramName).append("=").append(secret);
            hasQuery = true;
        }

        if (pageSize > 0) {
            urlBuilder.append(hasQuery ? "&" : "?").append("page=").append(page).append("&limit=").append(pageSize);
        }

        return URI.create(urlBuilder.toString());
    }

    private HttpRequest buildRequest(URI uri, CatalogIntegration integration, String secret) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json");

        String authType = integration.getAuthType() != null ? integration.getAuthType() : "NONE";
        if ("BEARER_TOKEN".equalsIgnoreCase(authType) && secret != null && !secret.isBlank()) {
            builder.header("Authorization", "Bearer " + secret);
        } else if ("API_KEY_HEADER".equalsIgnoreCase(authType) && secret != null && !secret.isBlank()) {
            String headerName = (integration.getApiKeyHeaderName() != null && !integration.getApiKeyHeaderName().isBlank())
                    ? integration.getApiKeyHeaderName() : "X-API-Key";
            builder.header(headerName, secret);
        }

        return builder.GET().build();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseResponse(String body) {
        if (body == null || body.isBlank()) return Collections.emptyList();
        try {
            Object parsed = objectMapper.readValue(body, Object.class);
            if (parsed instanceof List<?> list) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        result.add((Map<String, Object>) map);
                    }
                }
                return result;
            } else if (parsed instanceof Map<?, ?> map) {
                // Try finding array inside common wrapper fields
                for (String key : Arrays.asList("products", "data", "items", "content", "results")) {
                    Object nested = map.get(key);
                    if (nested instanceof List<?> list) {
                        List<Map<String, Object>> result = new ArrayList<>();
                        for (Object item : list) {
                            if (item instanceof Map<?, ?> itemMap) {
                                result.add((Map<String, Object>) itemMap);
                            }
                        }
                        return result;
                    }
                }
                // Single object fallback
                return Collections.singletonList((Map<String, Object>) map);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to parse JSON response body", e);
            return Collections.emptyList();
        }
    }
}
