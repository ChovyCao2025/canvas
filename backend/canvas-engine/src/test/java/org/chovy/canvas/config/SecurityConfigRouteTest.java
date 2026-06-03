package org.chovy.canvas.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigRouteTest {

    @Test
    void opsRequiresAdmin() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/ops/canvas/1/cache/invalidate").build());
        WebFilterChainProxy security = securityProxy();

        StepVerifier.create(security.filter(exchange, ignored -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private WebFilterChainProxy securityProxy() {
        JwtAuthFilter jwtAuthFilter = mock(JwtAuthFilter.class);
        when(jwtAuthFilter.filter(any(), any()))
                .thenAnswer(invocation -> {
                    WebFilterChain chain = invocation.getArgument(1);
                    return chain.filter(invocation.getArgument(0));
                });
        SecurityWebFilterChain chain = new SecurityConfig()
                .securityWebFilterChain(ServerHttpSecurity.http(), jwtAuthFilter);
        return new WebFilterChainProxy(chain);
    }
}
