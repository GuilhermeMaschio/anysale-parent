package com.anysale.lead.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.servlet.http.HttpServletRequest;

@Component
@ControllerAdvice
@RequiredArgsConstructor
public class IdempotencyResponseAdvice implements ResponseBodyAdvice<Object> {

    private final IdempotencyService service;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        // sempre permite; vamos checar o meta na hora de escrever
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class converterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest sreq)) return body;
        HttpServletRequest httpReq = sreq.getServletRequest();

        Object metaObj = httpReq.getAttribute(IdempotencyInterceptor.ATTR_META);
        if (metaObj instanceof IdempotencyInterceptor.Meta meta) {
            // salva resposta para a chave
            String etag = response.getHeaders().getETag();
            String location = (response.getHeaders().getLocation() == null) ? null
                    : response.getHeaders().getLocation().toString();
            int status = (response instanceof org.springframework.http.server.ServletServerHttpResponse sr)
                    ? sr.getServletResponse().getStatus()
                    : HttpStatus.OK.value();

            // Se o body já for ResponseEntity, pegue o status real
            if (body instanceof ResponseEntity<?> re) status = re.getStatusCode().value();

            ResponseEntity<?> re = (body instanceof ResponseEntity<?> rex) ? rex :
                    ResponseEntity.status(status).contentType(contentType).body(body);

            service.save(meta.op(), meta.resourceId(), meta.key(), meta.reqHash(),
                    re, meta.ttlSeconds(), etag, location, (contentType != null ? contentType.toString() : null));
        }
        return body;
    }
}
