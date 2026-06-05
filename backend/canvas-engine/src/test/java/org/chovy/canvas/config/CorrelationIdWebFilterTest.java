package org.chovy.canvas.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdWebFilterTest {

    private final CorrelationIdWebFilter filter = new CorrelationIdWebFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void preservesIncomingCorrelationIdAcrossResponseMdcAndReactorContext() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/canvas")
                        .header(CorrelationIdWebFilter.HEADER_NAME, "client-trace-123")
                        .build());
        AtomicReference<String> observedMdc = new AtomicReference<>();
        AtomicReference<String> observedContext = new AtomicReference<>();
        WebFilterChain chain = ignored -> Mono.deferContextual(context -> {
            observedMdc.set(MDC.get(CorrelationIdWebFilter.MDC_KEY));
            observedContext.set(context.get(CorrelationIdWebFilter.REACTOR_CONTEXT_KEY));
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdWebFilter.HEADER_NAME))
                .isEqualTo("client-trace-123");
        String attributeTraceId = exchange.getAttribute(CorrelationIdWebFilter.EXCHANGE_ATTRIBUTE);
        assertThat(attributeTraceId).isEqualTo("client-trace-123");
        assertThat(observedMdc.get()).isEqualTo("client-trace-123");
        assertThat(observedContext.get()).isEqualTo("client-trace-123");
        assertThat(MDC.get(CorrelationIdWebFilter.MDC_KEY)).isNull();
    }

    @Test
    void generatesCorrelationIdWhenHeaderIsMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/canvas").build());
        AtomicReference<String> observedMdc = new AtomicReference<>();
        WebFilterChain chain = ignored -> Mono.fromRunnable(() ->
                observedMdc.set(MDC.get(CorrelationIdWebFilter.MDC_KEY)));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        String traceId = exchange.getResponse().getHeaders()
                .getFirst(CorrelationIdWebFilter.HEADER_NAME);
        assertThat(traceId).isNotBlank();
        assertThat(UUID.fromString(traceId).toString()).isEqualTo(traceId);
        String attributeTraceId = exchange.getAttribute(CorrelationIdWebFilter.EXCHANGE_ATTRIBUTE);
        assertThat(attributeTraceId).isEqualTo(traceId);
        assertThat(observedMdc.get()).isEqualTo(traceId);
    }

    @Test
    void restoresPreviousMdcTraceIdAfterRequestCompletes() {
        MDC.put(CorrelationIdWebFilter.MDC_KEY, "outer-trace");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/canvas")
                        .header(CorrelationIdWebFilter.HEADER_NAME, "request-trace")
                        .build());
        AtomicReference<String> observedMdc = new AtomicReference<>();
        WebFilterChain chain = ignored -> Mono.fromRunnable(() ->
                observedMdc.set(MDC.get(CorrelationIdWebFilter.MDC_KEY)));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(observedMdc.get()).isEqualTo("request-trace");
        assertThat(MDC.get(CorrelationIdWebFilter.MDC_KEY)).isEqualTo("outer-trace");
    }
}
