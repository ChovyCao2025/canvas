package org.chovy.canvas.engine.rule;

import java.util.Locale;

/**
 * RuleOperator 枚举 engine.rule 场景中的固定业务取值。
 */
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

    /**
     * parse 校验或转换 engine.rule 场景的数据。
     * @param raw raw 参数，用于 parse 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public static RuleOperator parse(Object raw) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (raw == null) {
            throw new RuleValidationException("Unsupported operator: null");
        }
        String value = String.valueOf(raw).trim();
        if (value.isEmpty()) {
            throw new RuleValidationException("Unsupported operator: " + raw);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * isOrderComparison 校验或转换 engine.rule 场景的数据。
     * @return 返回布尔判断结果。
     */
    public boolean isOrderComparison() {
        return this == GT || this == GTE || this == LT || this == LTE;
    }
}
