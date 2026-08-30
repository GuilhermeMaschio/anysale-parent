package com.anysale.lead.adapters.in.rest.command;

import com.anysale.lead.aplication.CadenceEnrollmentService;
import com.anysale.lead.domain.model.CadenceEnrollmentSettings;
import com.anysale.lead.tenant.TenantContext;
import java.time.Instant;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/v1/cadence-enrollment-settings")
public class CadenceEnrollmentSettingsController {
    private final CadenceEnrollmentService service; private final TenantContext tenants;
    public CadenceEnrollmentSettingsController(CadenceEnrollmentService service, TenantContext tenants) { this.service=service; this.tenants=tenants; }
    @GetMapping public Response get() { return dto(service.settings(tenants.tenantId())); }
    @PutMapping public Response update(@RequestBody Request request) { return dto(service.update(tenants.tenantId(), request.enabled())); }
    private static Response dto(CadenceEnrollmentSettings value) { return new Response(value.isEnabled(), value.getUpdatedAt()); }
    public record Request(boolean enabled) {} public record Response(boolean enabled, Instant updatedAt) {}
}
