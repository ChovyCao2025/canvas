package org.chovy.canvas.engine.handlers;

import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 阈值触发节点（THRESHOLD）。
 *
 * <h3>与 AGGREGATE / HUB 的核心区别</h3>
 * <pre>
 * HUB       — 等所有上游完成，然后路由（handler 不读 ctx，repeat 无实际语义价值）
 * AGGREGATE — 等所有上游完成，然后读 ctx 评估（handler 读 ctx，但 ctx 此时已完整）
 * THRESHOLD — 不等，每个上游完成都触发一次评估。handler 读 ctx 时上游可能还未全部到达。
 *             这是 repeat 机制真正有语义价值的场景：
 *               第2个上游完成 → handler 持锁运行 → 计数=2 < 3 → 返回 waiting()
 *               第3个上游在持锁期间完成 → CAS 失败 → repeatPending=true
 *               handler 返回 → repeat 触发 → 重新计数=3 ≥ threshold → 路由 ✓
 *             没有 repeat：第3个上游的信号永久丢失，节点卡在 WAITING 直到超时。
 * </pre>
 *
 * <h3>阈值模式（thresholdMode）</h3>
 * <ul>
 *   <li>{@code min_success} — 成功数 ≥ N（默认，适合 K-of-N 投票）</li>
 *   <li>{@code min_done}    — 完成数 ≥ N（SUCCESS + FAILED 均计，适合"先到先得"）</li>
 *   <li>{@code any_fail}    — 任意上游失败立刻路由到 failNodeId</li>
 * </ul>
 *
 * <h3>路由</h3>
 * <ul>
 *   <li>达到阈值 → {@code successNodeId}</li>
 *   <li>全部完成但未达阈值 → {@code failNodeId}</li>
 *   <li>阈值未达且上游未全完成 → {@link NodeResult#waiting()}（继续等待）</li>
 * </ul>
 */
@Slf4j
@Component
@NodeHandlerType("THRESHOLD")
public class ThresholdHandler implements NodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        List<String> upstreamIds = (List<String>) config.get("__upstreamIds");
        String nodeId        = (String) config.get("__nodeId");
        String thresholdMode = (String) config.getOrDefault("thresholdMode", "min_success");
        int    threshold     = config.get("threshold") instanceof Number n ? n.intValue() : 1;
        String successNodeId = (String) config.get("successNodeId");
        String failNodeId    = (String) config.get("failNodeId");

        if (upstreamIds == null || upstreamIds.isEmpty()) {
            return Mono.just(NodeResult.ok(successNodeId, Map.of()));
        }

        // ── 读取当前 ctx 状态（repeat 时能看到更多上游的结果）────────
        long totalCount   = upstreamIds.size();
        long successCount = upstreamIds.stream()
                .filter(id -> ctx.getNodeStatus(id) == NodeStatus.SUCCESS).count();
        long doneCount    = upstreamIds.stream().filter(ctx::isNodeDone).count();
        boolean allDone   = doneCount >= totalCount;

        // ── 评估阈值 ────────────────────────────────────────────────
        boolean thresholdMet = switch (thresholdMode) {
            case "min_success" -> successCount >= threshold;
            case "min_done"    -> doneCount    >= threshold;
            case "any_fail"    -> upstreamIds.stream()
                    .anyMatch(id -> ctx.getNodeStatus(id) == NodeStatus.FAILED);
            default -> {
                log.warn("[THRESHOLD] 未知 thresholdMode={}", thresholdMode);
                yield false;
            }
        };

        log.debug("[THRESHOLD] nodeId={} mode={} success={} done={}/{} thresholdMet={}",
                nodeId, thresholdMode, successCount, doneCount, totalCount, thresholdMet);

        if (thresholdMet) {
            return Mono.just(NodeResult.ok(successNodeId, Map.of(
                    "successCount", successCount,
                    "doneCount",    doneCount,
                    "totalCount",   totalCount
            )));
        }

        if (allDone) {
            // 所有上游完成但阈值未达：路由到失败分支
            return Mono.just(NodeResult.ok(failNodeId, Map.of(
                    "successCount", successCount,
                    "doneCount",    doneCount,
                    "totalCount",   totalCount
            )));
        }

        // 阈值未达，还有上游未完成：挂起等待
        // repeat 机制保证：若在本次 handler 持锁期间有新上游完成，
        // 该上游的 CAS 失败会设置 repeatPending，handler 返回后 repeat 自动重新评估
        return Mono.just(NodeResult.waiting());
    }
}
