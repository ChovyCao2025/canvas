package org.chovy.canvas.engine.rule;

public record RuleCondition(String field, RuleOperator operator, Object value) implements RuleNode {
}
