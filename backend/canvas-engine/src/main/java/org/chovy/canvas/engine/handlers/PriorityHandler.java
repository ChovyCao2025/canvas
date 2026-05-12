package org.chovy.canvas.engine.handlers;

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

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        List<Map<String, Object>> priorities = (List<Map<String, Object>>) config.get("priorities");
        String nextNodeId = (String) config.get("nextNodeId");

        if (priorities == null || priorities.isEmpty()) {
            return nextNodeId != null ? NodeResult.ok(nextNodeId, Map.of()) : NodeResult.terminal(Map.of());
        }

        // 按 order 排序，返回第一个子节点 ID（调度器执行，成功后不执行后续）
        priorities.sort((a, b) -> {
            int oa = a.get("order") instanceof Number n ? n.intValue() : 0;
            int ob = b.get("order") instanceof Number n ? n.intValue() : 0;
            return Integer.compare(oa, ob);
        });

        // 返回所有子节点 ID，调度器按顺序依次尝试
        Map<String, String> branchMap = new HashMap<>();
        for (int i = 0; i < priorities.size(); i++) {
            String tid = (String) priorities.get(i).get("nextNodeId");
            if (tid != null) branchMap.put("priority-" + i, tid);
        }

        return Mono.just(NodeResult.multiNext(branchMap, nextNodeId));
    }
}
