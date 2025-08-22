package com.anysale.lead.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component @RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final IdempotencyService service;
    private final ObjectMapper mapper;

    public static final String ATTR_META = "IDEMPOTENCY_META";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) return true;

        Idempotent anno = hm.getMethodAnnotation(Idempotent.class);
        if (anno == null) return true;

        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) return true; // sem header, segue normal

        String op = anno.operation();
        UUID resourceId = extractResourceId(request, anno.resourceIdParam());
        String reqHash = hashRequestBody(request);

        var found = service.find(op, resourceId, key);
        if (found.isPresent()) {
            var rec = found.get();
            if (!rec.getRequestHash().equals(reqHash)) {
                problem(response, HttpStatus.CONFLICT, "Idempotency key reuse with different payload");
                return false;
            }
            // devolve do cache
            writeStoredResponse(response, rec);
            return false;
        }

        // guarda meta para o Advice salvar depois
        request.setAttribute(ATTR_META, new Meta(op, resourceId, key, reqHash, anno.ttlSeconds()));
        return true;
    }

    private static UUID extractResourceId(HttpServletRequest request, String param) {
        if (param == null || param.isBlank()) return null;
        @SuppressWarnings("unchecked")
        Map<String,String> vars = (Map<String,String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (vars == null) return null;
        String v = vars.get(param);
        return (v == null) ? null : UUID.fromString(v);
    }

    private static String hashRequestBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrap) {
            byte[] bytes = wrap.getContentAsByteArray();
            return IdempotencyService.sha256Hex(bytes);
        }
        // fallback (sem filter)
        return IdempotencyService.sha256Hex("");
    }

    private void problem(HttpServletResponse resp, HttpStatus status, String detail) throws IOException {
        resp.setStatus(status.value());
        resp.setContentType("application/problem+json");
        var body = mapper.createObjectNode()
                .put("type","about:blank")
                .put("title", status.getReasonPhrase())
                .put("status", status.value())
                .put("detail", detail);
        resp.getWriter().write(body.toString());
    }

    private void writeStoredResponse(HttpServletResponse resp, IdempotencyRecord rec) throws IOException {
        resp.setStatus(rec.getStatusCode());
        if (rec.getContentType() != null) resp.setContentType(rec.getContentType());
        if (rec.getEtag() != null) resp.setHeader("ETag", rec.getEtag());
        if (rec.getLocation() != null) resp.setHeader("Location", rec.getLocation());
        if (rec.getResponseBody() != null) resp.getWriter().write(rec.getResponseBody());
    }

    public record Meta(String op, UUID resourceId, String key, String reqHash, long ttlSeconds) {}
}
