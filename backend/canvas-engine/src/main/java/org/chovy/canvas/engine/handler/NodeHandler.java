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
 *
 * 约定：
 * - 成功路径返回 `NodeResult.ok/ifResult/multiNext/terminal`；
 * - 业务失败返回 `NodeResult.fail`，由调度层统一处理失败分支。
 */
public interface NodeHandler {

    /**
     * 执行节点主逻辑。
     *
     * @param config 节点配置（来自 DAG 节点 config/bizConfig）
     * @param ctx    当前执行上下文
     * @return 节点执行结果（成功路由 / 失败原因 / 输出上下文字段）
     */
    Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx);

    /** 是否权益发放类节点（用于统计/审计聚合，默认 false）。 */
    default boolean isBenefitNode() { return false; }

    /** 是否触达类节点（用于统计/审计聚合，默认 false）。 */
    default boolean isReachNode()   { return false; }

    // 这两个标记方法是“分类标签”，不影响节点执行语义本身。
}
