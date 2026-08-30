package com.anysale.catalog.adapters.out.http;

import com.anysale.catalog.application.connector.ConnectionTestResult;
import com.anysale.catalog.domain.model.CatalogIntegration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenericRestCatalogConnectorTest {

    private GenericRestCatalogConnector connector;
    private HttpServer mockServer;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        connector = new GenericRestCatalogConnector(new ObjectMapper());
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        port = mockServer.getAddress().getPort();
        mockServer.start();
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @Test
    void shouldSuccessfullyTestConnection() {
        mockServer.createContext("/api/products", exchange -> {
            String response = "[{\"id\": 1, \"name\": \"Item 1\"}]";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        });

        CatalogIntegration integration = new CatalogIntegration();
        integration.setBaseUrl("http://localhost:" + port + "/api/products");

        ConnectionTestResult result = connector.testConnection(integration, null);

        assertTrue(result.success());
        assertEquals(200, result.statusCode());
    }

    @Test
    void shouldFetchRawProductsWithBearerAuth() {
        mockServer.createContext("/api/products", exchange -> {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if ("Bearer test_token_123".equals(authHeader)) {
                String response = "[{\"sku\": \"ABC-123\", \"title\": \"Product ABC\", \"price\": 100}]";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
            } else {
                exchange.sendResponseHeaders(401, 0);
                exchange.close();
            }
        });

        CatalogIntegration integration = new CatalogIntegration();
        integration.setBaseUrl("http://localhost:" + port + "/api/products");
        integration.setAuthType("BEARER_TOKEN");

        List<Map<String, Object>> products = connector.fetchRawProducts(integration, "test_token_123", 1, 10);

        assertEquals(1, products.size());
        assertEquals("ABC-123", products.get(0).get("sku"));
    }
}
