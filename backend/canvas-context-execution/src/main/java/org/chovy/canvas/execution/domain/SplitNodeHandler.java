package org.chovy.canvas.execution.domain;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 定义 SplitNodeHandler 的执行上下文数据结构或业务契约。
 */
@Component
@NodeHandlerType("SPLIT")
public class SplitNodeHandler implements NodeHandler {

    /**
     * 执行 execute 对应的业务处理。
     * @param context context 参数
     * @return 处理后的结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        List<Map<String, Object>> branches = NodeHandlerSupport.listOfMaps(context.node().config().get("branches"));
        if (branches.isEmpty()) {
            return NodeExecutionResult.success(Map.of());
        }
        int selectedIndex = selectedIndex(branches);
        Map<String, Object> selected = branches.get(selectedIndex);
        String branchId = NodeHandlerSupport.string(
                selected.getOrDefault("branchId", selected.getOrDefault("id", selectedIndex)),
                String.valueOf(selectedIndex));
        String target = NodeHandlerSupport.string(selected.get("nextNodeId"), null);
        if (target == null) {
            return NodeExecutionResult.success(Map.of("splitBranch", branchId));
        }
        return NodeExecutionResult.routed(
                Map.of("splitBranch", branchId),
                Map.of("branch-" + branchId, target));
    }

    /**
     * 执行 selectedIndex 对应的业务处理。
     * @param branches branches 参数
     */
    private int selectedIndex(List<Map<String, Object>> branches) {
        for (int i = 0; i < branches.size(); i++) {
            if (NodeHandlerSupport.bool(branches.get(i).get("selected"))) {
                return i;
            }
        }
        return 0;
    }
}
