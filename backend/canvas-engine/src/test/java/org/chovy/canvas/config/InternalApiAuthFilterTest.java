package org.chovy.canvas.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class InternalApiAuthFilterTest {

    @Test
    void rejectsConfiguredInternalRouteWhenTokenIsMissing() {
        InternalApiAuthFilter filter = new InternalApiAuthFilter("secret");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/canvas/trigger/behavior").build());

        StepVerifier.create(filter.filter(exchange, e -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsConfiguredInternalRouteWhenTokenIsWrong() {
        InternalApiAuthFilter filter = new InternalApiAuthFilter("secret");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/canvas/execute/direct/demo")
                        .header(InternalApiAuthFilter.HEADER_NAME, "wrong")
                        .build());

        StepVerifier.create(filter.filter(exchange, e -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void allowsConfiguredInternalRouteWhenTokenMatches() {
        InternalApiAuthFilter filter = new InternalApiAuthFilter("secret");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/canvas/events/report")
                        .header(InternalApiAuthFilter.HEADER_NAME, "secret")
                        .build());
        AtomicBoolean invoked = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, e -> {
                    invoked.set(true);
                    return Mono.empty();
                }))
                .verifyComplete();

        assertThat(invoked).isTrue();
    }

    @Test
    void protectsRealtimeCheckpointRouteWithInternalToken() {
        InternalApiAuthFilter filter = new InternalApiAuthFilter("secret");
        MockServerWebExchange missing = MockServerWebExchange.from(
                MockServerHttpRequest.post("/warehouse/realtime/pipelines/checkpoints").build());
        MockServerWebExchange matching = MockServerWebExchange.from(
                MockServerHttpRequest.post("/warehouse/realtime/pipelines/checkpoints")
                        .header(InternalApiAuthFilter.HEADER_NAME, "secret")
                        .build());
        AtomicBoolean invoked = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(missing, e -> Mono.empty()))
                .verifyComplete();
        StepVerifier.create(filter.filter(matching, e -> {
                    invoked.set(true);
                    return Mono.empty();
                }))
                .verifyComplete();

        assertThat(missing.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(invoked).isTrue();
    }

    @Test
    void blankConfiguredTokenKeepsLocalCompatibility() {
        InternalApiAuthFilter filter = new InternalApiAuthFilter("");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/canvas/events/report").build());
        AtomicBoolean invoked = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, e -> {
                    invoked.set(true);
                    return Mono.empty();
                }))
                .verifyComplete();

        assertThat(invoked).isTrue();
    }
}
