package org.chovy.canvas.config;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * 请求关联 ID 入口过滤器。
 *
 * <p>统一从入口请求生成或读取关联 ID，并暴露到响应头、MDC、Reactor Context 和 exchange
 * attribute，供日志、异常响应和后续执行链路复用。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "traceId";
    public static final String REACTOR_CONTEXT_KEY = "traceId";
    public static final String EXCHANGE_ATTRIBUTE =
            CorrelationIdWebFilter.class.getName() + ".traceId";

    private static final int MAX_CORRELATION_ID_LENGTH = 128;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = resolveCorrelationId(exchange);
        exchange.getAttributes().put(EXCHANGE_ATTRIBUTE, correlationId);
        exchange.getResponse().getHeaders().set(HEADER_NAME, correlationId);

        return Mono.defer(() -> {
            String previousTraceId = MDC.get(MDC_KEY);
            MDC.put(MDC_KEY, correlationId);

            Mono<Void> filtered;
            try {
                filtered = chain.filter(exchange);
            } catch (RuntimeException | Error e) {
                restoreTraceId(previousTraceId);
                throw e;
            }

            return filtered
                    .contextWrite(context -> context.put(REACTOR_CONTEXT_KEY, correlationId))
                    .doFinally(ignored -> restoreTraceId(previousTraceId));
        });
    }

    public static Optional<String> currentTraceId() {
        return Optional.ofNullable(MDC.get(MDC_KEY))
                .filter(traceId -> !traceId.isBlank());
    }

    private String resolveCorrelationId(ServerWebExchange exchange) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (isUsableCorrelationId(incoming)) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString();
    }

    private boolean isUsableCorrelationId(String candidate) {
        if (candidate == null) {
            return false;
        }
        String trimmed = candidate.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_CORRELATION_ID_LENGTH) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isISOControl(trimmed.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void restoreTraceId(String previousTraceId) {
        if (previousTraceId == null) {
            MDC.remove(MDC_KEY);
            return;
        }
        MDC.put(MDC_KEY, previousTraceId);
    }
}
