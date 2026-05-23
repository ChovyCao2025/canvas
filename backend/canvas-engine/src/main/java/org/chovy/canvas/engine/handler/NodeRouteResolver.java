package org.chovy.canvas.engine.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NodeRouteResolver {
    private NodeRouteResolver() {
    }

    public static List<String> resolveTargets(NodeResult result) {
        List<String> targets = new ArrayList<>();
        if (result.routes() != null && !result.routes().isEmpty()) {
            String elseTarget = null;
            for (Map.Entry<String, String> entry : result.routes().entrySet()) {
                if ("__else".equals(entry.getKey())) {
                    elseTarget = entry.getValue();
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

    private static void addIfPresent(List<String> targets, String target) {
        if (target != null && !target.isBlank()) {
            targets.add(target);
        }
    }
}
