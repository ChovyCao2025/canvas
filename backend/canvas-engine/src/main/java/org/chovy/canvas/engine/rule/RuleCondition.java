package org.chovy.canvas.engine.rule;

/**
 * RuleCondition 承载 engine.rule 场景中的不可变数据快照。
 * @param field field 字段。
 * @param operator operator 字段。
 * @param value value 字段。
 */
public record RuleCondition(String field, RuleOperator operator, Object value) implements RuleNode {
}
