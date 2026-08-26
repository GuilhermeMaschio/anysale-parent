package com.anysale.catalog.application.connector;

import com.anysale.catalog.domain.model.CatalogIntegration;
import java.util.List;
import java.util.Map;

public interface CatalogProviderConnector {
    boolean supports(String providerType);
    ConnectionTestResult testConnection(CatalogIntegration integration, String decryptedSecret);
    List<Map<String, Object>> fetchRawProducts(CatalogIntegration integration, String decryptedSecret, int page, int pageSize);
}
