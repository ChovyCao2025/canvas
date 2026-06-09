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

    /**
     * filter 处理 config 场景的业务逻辑。
     * @param exchange exchange 参数，用于 filter 流程中的校验、计算或对象转换。
     * @param chain chain 参数，用于 filter 流程中的校验、计算或对象转换。
     * @return 返回 filter 流程生成的业务结果。
     */
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
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (RuntimeException | Error e) {
                restoreTraceId(previousTraceId);
                throw e;
            }

            return filtered
                    .contextWrite(context -> context.put(REACTOR_CONTEXT_KEY, correlationId))
                    .doFinally(ignored -> restoreTraceId(previousTraceId));
        });
    }

    /**
     * currentTraceId 处理 config 场景的业务逻辑。
     * @return 返回 current trace id 生成的文本或业务键。
     */
    public static Optional<String> currentTraceId() {
        return Optional.ofNullable(MDC.get(MDC_KEY))
                .filter(traceId -> !traceId.isBlank());
    }

    /**
     * 从请求头读取可用关联 ID，缺失或非法时生成新的 UUID。
     *
     * @param exchange 当前请求交换对象
     * @return 本次请求使用的关联 ID
     */
    private String resolveCorrelationId(ServerWebExchange exchange) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (isUsableCorrelationId(incoming)) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString();
    }

    /**
     * 判断入口关联 ID 是否非空、长度可控且不包含控制字符。
     *
     * @param candidate 请求头中的关联 ID 候选值
     * @return true 表示可以沿用该关联 ID
     */
    private boolean isUsableCorrelationId(String candidate) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (candidate == null) {
            return false;
        }
        String trimmed = candidate.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_CORRELATION_ID_LENGTH) {
            return false;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int i = 0; i < trimmed.length(); i++) {
            if (Character.isISOControl(trimmed.charAt(i))) {
                return false;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return true;
    }

    /**
     * 请求结束后恢复或清理 MDC 中的 traceId，避免线程复用造成串号。
     *
     * @param previousTraceId 进入过滤器前的 traceId
     */
    private void restoreTraceId(String previousTraceId) {
        if (previousTraceId == null) {
            MDC.remove(MDC_KEY);
            return;
        }
        MDC.put(MDC_KEY, previousTraceId);
    }
}
