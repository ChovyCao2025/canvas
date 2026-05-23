package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 优先级节点：按 priorities[].order 顺序尝试子节点，第一个 SUCCESS 即止。
 * 全部失败时，有 nextNodeId 则走 nextNodeId（标记 PARTIAL_FAIL），否则 FAILED。
 * 调度器在执行完每个子节点后回调此 Handler 判断是否继续。
 */
@Component
@NodeHandlerType("PRIORITY")
public class PriorityHandler implements NodeHandler {

    /**
     * 优先级节点执行入口。
     *
     * <p>本节点不直接执行子分支，只返回有序分支集合给调度器逐个尝试。
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // priorities 中每个元素对应一个候选分支，按 order 升序尝试
        List<Map<String, Object>> priorities = (List<Map<String, Object>>) config.get(MapFieldKeys.PRIORITIES);
        String nextNodeId = (String) config.get(MapFieldKeys.NEXT_NODE_ID);

        if (priorities == null || priorities.isEmpty()) {
            // 未配置优先级分支时：有兜底 next 则走兜底，否则终止
            return nextNodeId != null ? Mono.just(NodeResult.ok(nextNodeId, Map.of())) : Mono.just(NodeResult.terminal(Map.of()));
        }

        // 按 order 排序，返回第一个子节点 ID（调度器执行，成功后不执行后续）
        priorities.sort((a, b) -> {
            int oa = a.get("order") instanceof Number n ? n.intValue() : 0;
            int ob = b.get("order") instanceof Number n ? n.intValue() : 0;
            return Integer.compare(oa, ob);
        });

        // 返回“有序候选分支集合”，由调度器逐个执行直到命中成功分支
        Map<String, String> branchMap = new HashMap<>();
        for (int i = 0; i < priorities.size(); i++) {
            String tid = (String) priorities.get(i).get(MapFieldKeys.NEXT_NODE_ID);
            if (tid != null) branchMap.put("priority-" + i, tid);
        }

        // nextNodeId 作为“全部候选失败时”的兜底出口
        return Mono.just(NodeResult.multiNext(branchMap, nextNodeId));
    }
}
