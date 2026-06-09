package org.chovy.canvas.engine.rule;

import java.util.List;

/**
 * RuleGroup 承载 engine.rule 场景中的不可变数据快照。
 * @param logic logic 字段。
 * @param children children 字段。
 * @param explicitMatchAll explicitMatchAll 字段。
 */
public record RuleGroup(RuleLogic logic, List<RuleNode> children, boolean explicitMatchAll) implements RuleNode {
    public RuleGroup {
        logic = logic == null ? RuleLogic.AND : logic;
        children = children == null ? List.of() : List.copyOf(children);
    }

    /**
     * isEmpty 校验或转换 engine.rule 场景的数据。
     * @return 返回布尔判断结果。
     */
    public boolean isEmpty() {
        return children.isEmpty();
    }
}
