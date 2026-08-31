package com.anysale.lead.aplication;

import com.anysale.lead.adapters.in.rest.dto.ManagedUserCreateRequest;
import com.anysale.lead.adapters.in.rest.dto.ManagedUserResponse;
import com.anysale.lead.adapters.in.rest.dto.ManagedUserUpdateRequest;
import com.anysale.lead.config.KeycloakAdminProperties;
import com.anysale.lead.tenant.TenantContext;
import com.anysale.lead.tenant.UserIdentityContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class KeycloakUserManagementService {
    private static final Set<String> ANYSALE_ROLES = Set.of("ADMIN", "SALES_MANAGER", "SALES_AGENT");
    private static final List<String> ROLE_ORDER = List.of("ADMIN", "SALES_MANAGER", "SALES_AGENT");
    private final KeycloakAdminProperties properties;
    private final TenantContext tenantContext;
    private final UserIdentityContext userIdentityContext;
    private final RestClient client = RestClient.create();

    public KeycloakUserManagementService(KeycloakAdminProperties properties, TenantContext tenantContext,
                                         UserIdentityContext userIdentityContext) {
        this.properties = properties;
        this.tenantContext = tenantContext;
        this.userIdentityContext = userIdentityContext;
    }

    public List<ManagedUserResponse> list(String search) {
        ensureConfigured();
        String tenantId = tenantContext.tenantId();
        String uri = UriComponentsBuilder.fromUriString(adminBase() + "/users")
                .queryParam("max", 100)
                .queryParam("q", "tenant_id:" + tenantId)
                .queryParamIfPresent("search", StringUtils.hasText(search) ? java.util.Optional.of(search.trim()) : java.util.Optional.empty())
                .toUriString();
        List<KeycloakUser> users = request(() -> client.get().uri(uri).headers(this::authorization)
                .retrieve().body(new ParameterizedTypeReference<List<KeycloakUser>>() {}));
        return safe(users).stream()
                .filter(user -> belongsToTenant(user, tenantId))
                .map(this::toResponse)
                .toList();
    }

    public ManagedUserResponse create(ManagedUserCreateRequest request) {
        ensureConfigured();
        String tenantId = tenantContext.tenantId();
        Map<String, Object> payload = Map.of(
                "username", request.email().trim().toLowerCase(),
                "email", request.email().trim().toLowerCase(),
                "firstName", request.firstName().trim(),
                "lastName", request.lastName().trim(),
                "enabled", true,
                "emailVerified", false,
                "attributes", Map.of("tenant_id", List.of(tenantId))
        );
        URI location = request(() -> client.post().uri(adminBase() + "/users").headers(this::authorization)
                .body(payload).retrieve().toBodilessEntity().getHeaders().getLocation());
        if (location == null) throw unavailable("O Keycloak não retornou o identificador do novo usuário.");
        String id = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
        replaceRole(id, request.role());
        request(() -> client.put().uri(adminBase() + "/users/{id}/reset-password", id).headers(this::authorization)
                .body(Map.of("type", "password", "value", request.temporaryPassword(), "temporary", true))
                .retrieve().toBodilessEntity());
        return find(id, tenantId);
    }

    public ManagedUserResponse update(String id, ManagedUserUpdateRequest request) {
        ensureConfigured();
        String tenantId = tenantContext.tenantId();
        ManagedUserResponse currentUser = find(id, tenantId);
        ensureAdminAccessIsRetained(currentUser, "ADMIN".equals(request.role()) && request.enabled());
        request(() -> client.put().uri(adminBase() + "/users/{id}", id).headers(this::authorization)
                .body(Map.of(
                        "email", request.email().trim().toLowerCase(),
                        "username", request.email().trim().toLowerCase(),
                        "firstName", request.firstName().trim(),
                        "lastName", request.lastName().trim(),
                        "enabled", request.enabled(),
                        "attributes", Map.of("tenant_id", List.of(tenantId))
                )).retrieve().toBodilessEntity());
        replaceRole(id, request.role());
        return find(id, tenantId);
    }

    public void delete(String id) {
        ensureConfigured();
        if (id.equals(userIdentityContext.userId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não pode excluir a própria conta.");
        }
        ManagedUserResponse currentUser = find(id, tenantContext.tenantId());
        ensureAdminAccessIsRetained(currentUser, false);
        request(() -> client.delete().uri(adminBase() + "/users/{id}", id).headers(this::authorization)
                .retrieve().toBodilessEntity());
    }

    private void ensureAdminAccessIsRetained(ManagedUserResponse currentUser, boolean remainsActiveAdmin) {
        if (!"ADMIN".equals(currentUser.role()) || !currentUser.enabled() || remainsActiveAdmin) return;
        long activeAdminCount = list(null).stream()
                .filter(user -> "ADMIN".equals(user.role()) && user.enabled())
                .count();
        if (activeAdminCount <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é possível alterar, desativar ou excluir o único administrador ativo da empresa.");
        }
    }

    private ManagedUserResponse find(String id, String tenantId) {
        KeycloakUser user = request(() -> client.get().uri(adminBase() + "/users/{id}", id).headers(this::authorization)
                .retrieve().body(KeycloakUser.class));
        if (user == null) throw unavailable("O Keycloak não retornou os dados do usuário.");
        if (!belongsToTenant(user, tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado nesta empresa.");
        }
        return toResponse(user);
    }

    private boolean belongsToTenant(KeycloakUser user, String tenantId) {
        return safe(user.attributes() == null ? null : user.attributes().get("tenant_id")).contains(tenantId);
    }

    private ManagedUserResponse toResponse(KeycloakUser user) {
        List<KeycloakRole> roles = request(() -> client.get().uri(adminBase() + "/users/{id}/role-mappings/realm", user.id()).headers(this::authorization)
                .retrieve().body(new ParameterizedTypeReference<List<KeycloakRole>>() {}));
        String role = ROLE_ORDER.stream().filter(candidate -> safe(roles).stream().anyMatch(item -> candidate.equals(item.name()))).findFirst().orElse("SALES_AGENT");
        return new ManagedUserResponse(user.id(), value(user.firstName()), value(user.lastName()), value(user.email()), role,
                Boolean.TRUE.equals(user.enabled()), user.createdTimestamp() == null ? null : Instant.ofEpochMilli(user.createdTimestamp()));
    }

    private void replaceRole(String userId, String selectedRole) {
        List<KeycloakRole> currentRoles = request(() -> client.get().uri(adminBase() + "/users/{id}/role-mappings/realm", userId).headers(this::authorization)
                .retrieve().body(new ParameterizedTypeReference<List<KeycloakRole>>() {}));
        List<KeycloakRole> removable = safe(currentRoles).stream().filter(role -> ANYSALE_ROLES.contains(role.name())).toList();
        if (!removable.isEmpty()) request(() -> client.method(org.springframework.http.HttpMethod.DELETE)
                .uri(adminBase() + "/users/{id}/role-mappings/realm", userId).headers(this::authorization)
                .body(removable).retrieve().toBodilessEntity());
        KeycloakRole role = request(() -> client.get().uri(adminBase() + "/roles/{role}", selectedRole).headers(this::authorization)
                .retrieve().body(KeycloakRole.class));
        if (role == null) throw unavailable("A role comercial não foi encontrada no Keycloak.");
        request(() -> client.post().uri(adminBase() + "/users/{id}/role-mappings/realm", userId).headers(this::authorization)
                .body(List.of(role)).retrieve().toBodilessEntity());
    }

    private void authorization(HttpHeaders headers) {
        headers.setBearerAuth(accessToken());
    }

    private String accessToken() {
        Map<String, Object> response = request(() -> client.post().uri(properties.issuerUri() + "/protocol/openid-connect/token")
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .body("grant_type=client_credentials&client_id=" + formValue(properties.clientId()) + "&client_secret=" + formValue(properties.clientSecret()))
                .retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() {}));
        Object token = response == null ? null : response.get("access_token");
        if (!(token instanceof String value) || value.isBlank()) throw unavailable("Não foi possível obter uma credencial de serviço do Keycloak.");
        return value;
    }

    private String adminBase() {
        URI issuer = URI.create(properties.issuerUri());
        String authority = issuer.getScheme() + "://" + issuer.getAuthority();
        return authority + "/admin/realms/" + properties.realm();
    }

    private void ensureConfigured() {
        if (!properties.enabled() || !StringUtils.hasText(properties.issuerUri()) || !StringUtils.hasText(properties.clientId()) || !StringUtils.hasText(properties.clientSecret())) {
            throw unavailable("O gerenciamento de usuários ainda não está configurado. Configure o cliente de serviço do Keycloak no Lead Service.");
        }
    }

    private <T> T request(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientException exception) {
            throw unavailable("Não foi possível comunicar com o Keycloak para gerenciar usuários.");
        }
    }

    private ResponseStatusException unavailable(String message) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    private static <T> List<T> safe(Collection<T> values) { return values == null ? List.of() : List.copyOf(values); }
    private static String value(String value) { return Objects.requireNonNullElse(value, ""); }
    private static String formValue(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    private record KeycloakUser(String id, String firstName, String lastName, String email, Boolean enabled,
                                Long createdTimestamp, Map<String, List<String>> attributes) {}
    private record KeycloakRole(String id, String name, String description, Boolean composite, Boolean clientRole, String containerId) {}
}
