package com.anysale.notification.internalauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalTokenInterceptor implements HandlerInterceptor {

    public static final String HEADER_NAME = "X-Internal-Token";

    private final String configuredToken;

    public InternalTokenInterceptor(
            @Value("${internal.auth.token:${ANYSALE_INTERNAL_TOKEN:}}") String configuredToken
    ) {
        this.configuredToken = configuredToken;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (!requiresInternalToken(handlerMethod) || configuredToken == null || configuredToken.isBlank()) {
            return true;
        }

        String providedToken = request.getHeader(HEADER_NAME);
        if (configuredToken.equals(providedToken)) {
            return true;
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid " + HEADER_NAME);
        return false;
    }

    private boolean requiresInternalToken(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), InternalTokenProtected.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), InternalTokenProtected.class);
    }
}
