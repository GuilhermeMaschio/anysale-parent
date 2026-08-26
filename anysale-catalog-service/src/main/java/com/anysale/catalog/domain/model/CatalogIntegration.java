package com.anysale.catalog.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;

@Document("catalog_integrations")
public class CatalogIntegration {
    @Id
    private String id;
    private String tenantId;
    private String name;
    private String providerType = "GENERIC_REST"; // GENERIC_REST
    private String baseUrl;
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, ERROR
    private String encryptedCredentials;
    private String authType = "NONE"; // NONE, BEARER_TOKEN, API_KEY_HEADER, API_KEY_QUERY
    private String apiKeyHeaderName = "X-API-Key";
    private String apiKeyQueryName = "api_key";
    private String syncMode = "MANUAL"; // MANUAL, SCHEDULED
    private String schedule;
    private Map<String, String> fieldMapping;
    private String conflictStrategy = "EXTERNAL_WINS"; // EXTERNAL_WINS, LOCAL_WINS, REVIEW_REQUIRED
    private boolean skuFallbackEnabled = true;
    private Instant lastSyncAt;
    private String lastSyncStatus;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEncryptedCredentials() { return encryptedCredentials; }
    public void setEncryptedCredentials(String encryptedCredentials) { this.encryptedCredentials = encryptedCredentials; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getApiKeyHeaderName() { return apiKeyHeaderName; }
    public void setApiKeyHeaderName(String apiKeyHeaderName) { this.apiKeyHeaderName = apiKeyHeaderName; }
    public String getApiKeyQueryName() { return apiKeyQueryName; }
    public void setApiKeyQueryName(String apiKeyQueryName) { this.apiKeyQueryName = apiKeyQueryName; }
    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }
    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }
    public Map<String, String> getFieldMapping() { return fieldMapping; }
    public void setFieldMapping(Map<String, String> fieldMapping) { this.fieldMapping = fieldMapping; }
    public String getConflictStrategy() { return conflictStrategy; }
    public void setConflictStrategy(String conflictStrategy) { this.conflictStrategy = conflictStrategy; }
    public boolean isSkuFallbackEnabled() { return skuFallbackEnabled; }
    public void setSkuFallbackEnabled(boolean skuFallbackEnabled) { this.skuFallbackEnabled = skuFallbackEnabled; }
    public Instant getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Instant lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public String getLastSyncStatus() { return lastSyncStatus; }
    public void setLastSyncStatus(String lastSyncStatus) { this.lastSyncStatus = lastSyncStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
