package org.chovy.canvas.engine.rule;

/**
 * RuleLogic 枚举 engine.rule 场景中的固定业务取值。
 */
public enum RuleLogic {
    AND,
    OR;

    /**
     * parse 校验或转换 engine.rule 场景的数据。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public static RuleLogic parse(Object value) {
        return "OR".equalsIgnoreCase(String.valueOf(value)) ? OR : AND;
    }
}
