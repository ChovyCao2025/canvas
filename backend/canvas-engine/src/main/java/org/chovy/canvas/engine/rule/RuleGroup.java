package org.chovy.canvas.engine.rule;

import java.util.List;

public record RuleGroup(RuleLogic logic, List<RuleNode> children, boolean explicitMatchAll) implements RuleNode {
    public RuleGroup {
        logic = logic == null ? RuleLogic.AND : logic;
        children = children == null ? List.of() : List.copyOf(children);
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }
}
