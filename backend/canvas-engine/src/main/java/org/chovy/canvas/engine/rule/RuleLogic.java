package org.chovy.canvas.engine.rule;

public enum RuleLogic {
    AND,
    OR;

    public static RuleLogic parse(Object value) {
        return "OR".equalsIgnoreCase(String.valueOf(value)) ? OR : AND;
    }
}
