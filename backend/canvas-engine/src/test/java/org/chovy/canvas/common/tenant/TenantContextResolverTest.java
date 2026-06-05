package org.chovy.canvas.common.tenant;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextResolverTest {

    private final TenantContextResolver resolver = new TenantContextResolver();

    @Test
    void currentExtractsClaimsFromReactiveSecurityContext() {
        Claims claims = claims("3");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                claims, null, List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")));

        StepVerifier.create(resolver.current()
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .assertNext(context -> {
                    assertThat(context.tenantId()).isEqualTo(3L);
                    assertThat(context.role()).isEqualTo(RoleNames.TENANT_ADMIN);
                    assertThat(context.username()).isEqualTo("tenant_admin");
                })
                .verifyComplete();
    }

    @Test
    void currentUsesNullTenantIdForInvalidStringClaim() {
        Claims claims = claims("invalid");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                claims, null, List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN")));

        StepVerifier.create(resolver.current()
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .assertNext(context -> assertThat(context.tenantId()).isNull())
                .verifyComplete();
    }

    @Test
    void currentOrErrorRejectsMissingTenantContext() {
        StepVerifier.create(resolver.currentOrError())
                .expectErrorMatches(error -> error instanceof SecurityException
                        && error.getMessage().equals("AUTH_003: missing tenant context"))
                .verify();
    }

    private Claims claims(String tenantId) {
        return Jwts.claims()
                .add("tenantId", tenantId)
                .add("role", RoleNames.TENANT_ADMIN)
                .add("username", "tenant_admin")
                .build();
    }
}
