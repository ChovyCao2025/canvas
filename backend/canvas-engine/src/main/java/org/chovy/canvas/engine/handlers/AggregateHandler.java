package org.chovy.canvas.engine.handlers;

import groovy.lang.Binding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聚合评估节点（AGGREGATE）。
 *
 * <p>与 HUB（只等待，不评估）不同，AGGREGATE 在等待所有上游完成后，
 * 基于上游的执行结果进行条件评估，并据此路由到不同的下游分支。
 *
 * <p><b>评估方式（evaluateMode）</b>：
 * <ul>
 *   <li>{@code count}  — 成功数 ≥ minCount</li>
 *   <li>{@code rate}   — 成功率 ≥ minRate（0~100）</li>
 *   <li>{@code script} — 自定义 Groovy 布尔表达式</li>
 * </ul>
 *
 * <p><b>脚本可用变量</b>（{@code evaluateMode=script} 时）：
 * <pre>
 *   successCount  — 上游 SUCCESS 数量
 *   failCount     — 上游非 SUCCESS 数量
 *   totalCount    — 上游总数
 *   successRate   — 成功率（0~100 的 double）
 *   outputs       — Map&lt;nodeId, Map&lt;String, Object&gt;&gt;：各上游的输出数据
 * </pre>
 *
 * <p><b>路由</b>：
 * <ul>
 *   <li>条件满足 → {@code successNodeId}</li>
 *   <li>条件不满足 → {@code failNodeId}</li>
 * </ul>
 *
 * <p><b>repeat 机制的真正用武之地</b>：
 * 多条上游并发完成时，后到的上游通过 {@code NodeGate.repeatPending} 通知先执行的一方
 * 在完成后重跑 handler。由于 handler 读取 ctx 中的上游状态来评估条件，
 * repeat 保证了评估时能看到所有上游的最终结果，而不是部分结果。
 * 这是该节点与 HUB（handler 不读 ctx）的本质区别。
 */
@Slf4j
@Component
@NodeHandlerType("AGGREGATE")
@RequiredArgsConstructor
public class AggregateHandler implements NodeHandler {

    /** script 模式复用 Groovy 表达式执行能力。 */
    private final GroovyHandler groovyHandler;

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // __upstreamIds / __nodeId 为调度器注入的内部字段
        List<String> upstreamIds = (List<String>) config.get(MapFieldKeys.UPSTREAM_IDS);
        String evaluateMode      = (String) config.getOrDefault("evaluateMode", "count");
        String successNodeId     = (String) config.get(MapFieldKeys.SUCCESS_NODE_ID);
        String failNodeId        = (String) config.get(MapFieldKeys.FAIL_NODE_ID);

        // ── 统计上游结果 ──────────────────────────────────────────
        long totalCount   = upstreamIds != null ? upstreamIds.size() : 0;
        long successCount = upstreamIds == null ? 0 : upstreamIds.stream()
                .filter(id -> ctx.getNodeStatus(id) == NodeStatus.SUCCESS)
                .count();
        long failCount    = totalCount - successCount;
        double successRate = totalCount > 0 ? (successCount * 100.0 / totalCount) : 0.0;

        // ── 收集上游输出（供 script 模式使用）────────────────────
        Map<String, Object> outputs = new HashMap<>();
        if (upstreamIds != null) {
            upstreamIds.forEach(id -> {
                Map<String, Object> out = ctx.getNodeOutputs().get(id);
                if (out != null) outputs.put(id, out);
            });
        }

        // ── 评估条件 ──────────────────────────────────────────────
        boolean passed = switch (evaluateMode) {
            case "count" -> {
                long minCount = config.get("minCount") instanceof Number n ? n.longValue() : 1L;
                yield successCount >= minCount;
            }
            case "rate" -> {
                double minRate = config.get("minRate") instanceof Number n ? n.doubleValue() : 100.0;
                yield successRate >= minRate;
            }
            case "script" -> {
                String script = (String) config.getOrDefault("evaluateScript", "false");
                Binding binding = new Binding();
                binding.setVariable("successCount", successCount);
                binding.setVariable("failCount",    failCount);
                binding.setVariable("totalCount",   totalCount);
                binding.setVariable("successRate",  Math.round(successRate * 10) / 10.0);
                binding.setVariable("outputs",      outputs);
                try {
                    // 脚本返回 true/false，非 true 一律按不通过处理
                    Object result = groovyHandler.evaluateExpression(script, binding);
                    yield Boolean.TRUE.equals(result);
                } catch (Exception e) {
                    log.error("[AGGREGATE] 脚本评估失败 nodeId={}: {}", config.get(MapFieldKeys.NODE_ID_INTERNAL), e.getMessage());
                    yield false;
                }
            }
            default -> {
                log.warn("[AGGREGATE] 未知 evaluateMode={}", evaluateMode);
                yield false;
            }
        };

        // ── 路由 ──────────────────────────────────────────────────
        String nextNodeId = passed ? successNodeId : failNodeId;
        log.debug("[AGGREGATE] mode={} successCount={}/{} rate={}% passed={} → {}",
                evaluateMode, successCount, totalCount,
                Math.round(successRate), passed, nextNodeId);

        return Mono.just(NodeResult.ok(nextNodeId, Map.of(
                MapFieldKeys.SUCCESS_COUNT, successCount,
                MapFieldKeys.FAIL_COUNT, failCount,
                MapFieldKeys.TOTAL_COUNT, totalCount,
                MapFieldKeys.SUCCESS_RATE, Math.round(successRate * 10) / 10.0,
                MapFieldKeys.PASSED, passed
        )));
    }
}
