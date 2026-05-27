package org.chovy.canvas.engine.rule;

import java.util.Locale;

public enum RuleOperator {
    EQ,
    NEQ,
    CONTAINS,
    IN,
    GT,
    LT,
    GTE,
    LTE,
    EXISTS,
    IS_EMPTY;

    public static RuleOperator parse(Object raw) {
        if (raw == null) {
            throw new RuleValidationException("Unsupported operator: null");
        }
        String value = String.valueOf(raw).trim();
        if (value.isEmpty()) {
            throw new RuleValidationException("Unsupported operator: " + raw);
        }
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "=", "EQ", "EQUALS" -> EQ;
            case "!=", "<>", "NEQ", "NOT_EQUALS" -> NEQ;
            case "CONTAINS" -> CONTAINS;
            case "IN" -> IN;
            case ">", "GT", "GREATER_THAN" -> GT;
            case "<", "LT", "LESS_THAN" -> LT;
            case ">=", "GTE", "GREATER_THAN_OR_EQUAL" -> GTE;
            case "<=", "LTE", "LESS_THAN_OR_EQUAL" -> LTE;
            case "EXISTS" -> EXISTS;
            case "IS_EMPTY", "EMPTY" -> IS_EMPTY;
            default -> throw new RuleValidationException("Unsupported operator: " + raw);
        };
    }

    public boolean isOrderComparison() {
        return this == GT || this == GTE || this == LT || this == LTE;
    }
}
