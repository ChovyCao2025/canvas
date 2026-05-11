package com.photon.canvas.engine.handlers;

import com.photon.canvas.engine.context.ExecutionContext;
import com.photon.canvas.engine.handler.NodeHandler;
import com.photon.canvas.engine.handler.NodeHandlerType;
import com.photon.canvas.engine.handler.NodeResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AB 分流：Hash(userId:experimentKey) % 100 确定性分流。
 * 相同用户 + 相同实验永远落入相同分组，无需外部状态。
 */
@NodeHandlerType("AB_SPLIT")
public class AbSplitHandler implements NodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        String experimentKey = (String) config.get("experimentKey");
        List<Map<String, Object>> groups = (List<Map<String, Object>>) config.get("groups");
        if (groups == null || groups.isEmpty()) return NodeResult.terminal(Map.of());

        int bucket = Math.abs((ctx.getUserId() + ":" + experimentKey).hashCode()) % 100;

        // 按分组配比决定命中哪组（groups 依次占满 100 个桶）
        int cursor = 0;
        for (Map<String, Object> group : groups) {
            int percent = group.get("percent") instanceof Number n ? n.intValue() : 0;
            cursor += percent;
            if (bucket < cursor) {
                String nextNodeId = (String) group.get("nextNodeId");
                String groupKey   = (String) group.get("groupKey");
                return NodeResult.ok(nextNodeId,
                        Map.of("abGroup", groupKey != null ? groupKey : ""));
            }
        }

        // 兜底取最后一组
        Map<String, Object> last = groups.get(groups.size() - 1);
        return NodeResult.ok((String) last.get("nextNodeId"),
                Map.of("abGroup", last.getOrDefault("groupKey", "")));
    }
}
