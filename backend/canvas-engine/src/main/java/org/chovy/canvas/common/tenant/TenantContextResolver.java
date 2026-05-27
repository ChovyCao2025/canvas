package org.chovy.canvas.common.tenant;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

public final class TenantContextResolver {

    private TenantContextResolver() {
    }

    public static Mono<TenantContext> current() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(Claims.class::isInstance)
                .cast(Claims.class)
                .map(TenantContextResolver::fromClaims);
    }

    private static TenantContext fromClaims(Claims claims) {
        return new TenantContext(
                readLong(claims.get("tenantId")),
                claims.get("role", String.class),
                claims.get("username", String.class));
    }

    private static Long readLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }
        return null;
    }
}
