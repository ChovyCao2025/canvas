package org.chovy.canvas.engine.handler;

import org.chovy.canvas.engine.context.ExecutionContext;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * 节点 Handler 接口（响应式）。
 *
 * 返回 Mono<NodeResult> 彻底消除 .block()：
 *   - 集成层（CouponHandler / ApiCallHandler 等）：直接返回 WebClient 响应式链
 *   - 简单 Handler（IF / Selector 等）：Mono.just(...) 包裹，无额外开销
 *   - 阻塞型（Groovy / Delay）：subscribeOn(virtualThreadScheduler) 调度到虚拟线程
 *
 * 实现类须同时标注 @Component 和 @NodeHandlerType("TYPE_KEY")。
 */
public interface NodeHandler {

    Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx);

    default boolean isBenefitNode() { return false; }
    default boolean isReachNode()   { return false; }
}
