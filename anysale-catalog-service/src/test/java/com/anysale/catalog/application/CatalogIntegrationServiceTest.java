package com.anysale.catalog.application;

import com.anysale.catalog.adapters.in.rest.dto.CatalogIntegrationRequest;
import com.anysale.catalog.adapters.in.rest.dto.CatalogIntegrationResponse;
import com.anysale.catalog.adapters.out.persistence.CatalogIntegrationRepository;
import com.anysale.catalog.adapters.out.persistence.CatalogSyncExecutionRepository;
import com.anysale.catalog.application.connector.CatalogProviderConnector;
import com.anysale.catalog.application.mapper.FieldMapper;
import com.anysale.catalog.domain.model.CatalogIntegration;
import com.anysale.catalog.security.CredentialEncryptionService;
import com.anysale.catalog.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CatalogIntegrationServiceTest {

    private CatalogIntegrationRepository repository;
    private CatalogSyncExecutionRepository executionRepository;
    private TenantContext tenantContext;
    private CredentialEncryptionService encryptionService;
    private CatalogIntegrationService service;

    @BeforeEach
    void setUp() {
        repository = mock(CatalogIntegrationRepository.class);
        executionRepository = mock(CatalogSyncExecutionRepository.class);
        tenantContext = mock(TenantContext.class);
        encryptionService = mock(CredentialEncryptionService.class);
        CatalogProviderConnector connector = mock(CatalogProviderConnector.class);
        FieldMapper fieldMapper = new FieldMapper();
        CatalogSyncProcessor syncProcessor = mock(CatalogSyncProcessor.class);

        when(tenantContext.tenantId()).thenReturn("tenant-alpha");
        when(encryptionService.encrypt(anyString())).thenReturn("enc_secret_123");

        service = new CatalogIntegrationService(
                repository, executionRepository, tenantContext, encryptionService, List.of(connector), fieldMapper, syncProcessor
        );
    }

    @Test
    void shouldCreateIntegrationAndRedactSecretInResponse() {
        CatalogIntegrationRequest req = new CatalogIntegrationRequest(
                "ERP Vendas", "GENERIC_REST", "https://erp.com/api", "ACTIVE",
                "super_secret_token", "BEARER_TOKEN", null, null, "MANUAL", null,
                Map.of("sku", "code"), "EXTERNAL_WINS", true
        );

        when(repository.save(any(CatalogIntegration.class))).thenAnswer(invocation -> {
            CatalogIntegration entity = invocation.getArgument(0);
            entity.setId("integration-100");
            return entity;
        });

        CatalogIntegrationResponse response = service.create(req);

        assertEquals("integration-100", response.id());
        assertEquals("tenant-alpha", response.tenantId());
        assertEquals("ERP Vendas", response.name());
        assertTrue(response.hasCredentials());
    }

    @Test
    void shouldListIntegrationsFilteredByTenant() {
        CatalogIntegration item = new CatalogIntegration();
        item.setId("int-1");
        item.setTenantId("tenant-alpha");
        item.setName("ERP Alpha");

        when(repository.findByTenantIdOrderByCreatedAtDesc("tenant-alpha")).thenReturn(List.of(item));

        List<CatalogIntegrationResponse> list = service.list();

        assertEquals(1, list.size());
        assertEquals("int-1", list.get(0).id());
        assertEquals("tenant-alpha", list.get(0).tenantId());
        verify(repository).findByTenantIdOrderByCreatedAtDesc("tenant-alpha");
    }
}
