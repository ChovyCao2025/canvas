package org.chovy.canvas.engine.handler;

import org.chovy.canvas.common.MapFieldKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Node Route Resolver 执行引擎基础类型。
 *
 * <p>定义节点执行结果、路由判断或处理器契约，是 DAG 调度与具体节点实现之间的稳定接口。
 * <p>该层不依赖具体业务节点，保持通用语义以便新增节点复用。
 */
public final class NodeRouteResolver {
    private NodeRouteResolver() {
    }

    public static List<String> resolveTargets(NodeResult result) {
        List<String> targets = new ArrayList<>();
        if (result.routes() != null && !result.routes().isEmpty()) {
            String elseTarget = resolveFallbackTarget(result);
            for (Map.Entry<String, String> entry : result.routes().entrySet()) {
                if (MapFieldKeys.ELSE.equals(entry.getKey())) {
                    continue;
                } else {
                    addIfPresent(targets, entry.getValue());
                }
            }
            addIfPresent(targets, elseTarget);
            return targets;
        }
        addIfPresent(targets, result.nextNodeId());
        addIfPresent(targets, result.successNodeId());
        addIfPresent(targets, result.failNodeId());
        addIfPresent(targets, result.elseNodeId());
        if (result.branchMap() != null) {
            for (Map.Entry<String, String> entry : result.branchMap().entrySet()) {
                addIfPresent(targets, entry.getValue());
            }
        }
        return targets;
    }

    public static List<String> resolvePriorityBranchTargets(NodeResult result) {
        List<String> targets = new ArrayList<>();
        if (result.routes() != null && !result.routes().isEmpty()) {
            for (Map.Entry<String, String> entry : result.routes().entrySet()) {
                if (!MapFieldKeys.ELSE.equals(entry.getKey())) {
                    addIfPresent(targets, entry.getValue());
                }
            }
            return targets;
        }
        if (result.branchMap() != null) {
            for (Map.Entry<String, String> entry : result.branchMap().entrySet()) {
                addIfPresent(targets, entry.getValue());
            }
        }
        return targets;
    }

    public static String resolveFallbackTarget(NodeResult result) {
        String target = null;
        if (result.routes() != null && result.routes().containsKey(MapFieldKeys.ELSE)) {
            target = result.routes().get(MapFieldKeys.ELSE);
        } else {
            target = result.elseNodeId();
        }
        return target == null || target.isBlank() ? null : target;
    }

    private static void addIfPresent(List<String> targets, String target) {
        if (target != null && !target.isBlank()) {
            targets.add(target);
        }
    }
}
