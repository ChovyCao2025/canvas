package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * AB 分流节点（设计文档 4.3 节）。
 *
 * 算法：Hash(userId:experimentKey) % 100 确定性分流。
 * groups 数组只含 groupKey 和 nextNodeId（设计文档规范，无 percent 字段）。
 * 分组策略：100 个桶按 groups 数量等比分配。
 *   2组：[0,50) → A，[50,100) → B
 *   3组：[0,33) → A，[33,66) → B，[66,100) → C
 *
 * 若需自定义比例，在 ABTest 系统维护后由其返回分组，canvas 侧不硬编码比例。
 */
@Component
@NodeHandlerType("AB_SPLIT")
public class AbSplitHandler implements NodeHandler {

    /**
     * AB 分流执行。
     *
     * <p>输入依赖：
     * - `experimentKey`：实验标识；
     * - `groups`：分组列表，每项至少包含 `groupKey` 与 `nextNodeId`。
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // experimentKey 参与 hash，保证同一用户在同一实验内分流稳定
        String experimentKey = (String) config.get("experimentKey");
        List<Map<String, Object>> groups =
                (List<Map<String, Object>>) config.get("groups");

        if (groups == null || groups.isEmpty()) {
            // 无分组配置时终止，避免进入未知分支
            return Mono.just(NodeResult.terminal(Map.of()));
        }

        // 确定性 Hash 分桶（相同 userId + experimentKey 永远同组）
        int bucket = Math.abs((ctx.getUserId() + ":" + experimentKey).hashCode()) % 100;

        // 等比分桶：n 组 → 每组覆盖 100/n 个桶
        int groupCount = groups.size();
        int groupIndex = Math.min(bucket * groupCount / 100, groupCount - 1);

        Map<String, Object> matched = groups.get(groupIndex);
        String nextNodeId = (String) matched.get("nextNodeId");
        String groupKey   = (String) matched.getOrDefault("groupKey", String.valueOf(groupIndex));

        // 将分组结果写入输出，供后续节点做实验分组埋点/差异化处理
        return Mono.just(NodeResult.ok(nextNodeId, Map.of("abGroup", groupKey)));
    }
}
