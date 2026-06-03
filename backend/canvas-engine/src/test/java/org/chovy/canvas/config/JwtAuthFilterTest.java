package org.chovy.canvas.config;

import io.jsonwebtoken.Claims;
import org.chovy.canvas.auth.domain.SysUserService;
import org.chovy.canvas.auth.util.JwtUtil;
import org.chovy.canvas.dal.dataobject.SysUserDO;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.WebFilterChain;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    private static final String SECRET = "test-jwt-secret-at-least-32-bytes-long";

    @Test
    void filterCopiesImmutableParsedClaimsBeforeAddingCurrentUserFields() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 24);
        String token = jwtUtil.generate(user(7L, 3L, "stale", "USER", "Stale User", 1));

        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.hasKey(anyString())).thenReturn(false);
        SysUserService userService = mock(SysUserService.class);
        when(userService.findById(7L))
                .thenReturn(user(7L, 3L, "alice", "ADMIN", "Alice", 1));

        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil, redis, userService);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/canvas/home/overview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        AtomicReference<Authentication> authentication = new AtomicReference<>();
        WebFilterChain chain = ignored -> ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .doOnNext(authentication::set)
                .then();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(authentication.get()).isNotNull();
        assertThat(authentication.get().getPrincipal()).isInstanceOf(Claims.class);
        Claims principal = (Claims) authentication.get().getPrincipal();
        assertThat(principal.getSubject()).isEqualTo("7");
        assertThat(principal.get("username", String.class)).isEqualTo("alice");
        assertThat(principal.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(principal.get("displayName", String.class)).isEqualTo("Alice");
        assertThat(principal.get("tenantId", Number.class).longValue()).isEqualTo(3L);
    }

    @Test
    void filterUsesCurrentDatabaseTenantInsteadOfStaleTokenTenant() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 24);
        String token = jwtUtil.generate(user(7L, 3L, "stale", "USER", "Stale User", 1));

        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.hasKey(anyString())).thenReturn(false);
        SysUserService userService = mock(SysUserService.class);
        when(userService.findById(7L))
                .thenReturn(user(7L, 42L, "alice", "TENANT_ADMIN", "Alice", 1));

        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil, redis, userService);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/canvas/home/overview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        AtomicReference<Authentication> authentication = new AtomicReference<>();
        WebFilterChain chain = ignored -> ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .doOnNext(authentication::set)
                .then();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        Claims principal = (Claims) authentication.get().getPrincipal();
        assertThat(principal.get("tenantId", Number.class).longValue()).isEqualTo(42L);
    }

    private static SysUserDO user(Long id, Long tenantId, String username,
                                  String role, String displayName, Integer enabled) {
        SysUserDO user = new SysUserDO();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setRole(role);
        user.setDisplayName(displayName);
        user.setEnabled(enabled);
        return user;
    }
}
