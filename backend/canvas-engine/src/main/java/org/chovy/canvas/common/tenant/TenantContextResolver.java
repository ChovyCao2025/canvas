package org.chovy.canvas.common.tenant;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TenantContextResolver {

    public Mono<TenantContext> current() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(Claims.class::isInstance)
                .cast(Claims.class)
                .map(this::fromClaims);
    }

    public Mono<TenantContext> currentOrError() {
        return current().switchIfEmpty(Mono.error(
                new SecurityException("AUTH_003: missing tenant context")));
    }

    private TenantContext fromClaims(Claims claims) {
        return new TenantContext(
                readLong(claims.get("tenantId")),
                claims.get("role", String.class),
                claims.get("username", String.class));
    }

    private Long readLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return Long.valueOf(trimmed);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
