package org.chovy.canvas.risk.domain.dsl;

import java.util.Arrays;
import java.util.Optional;

/**
 * 风控规则操作符，同时保留枚举名和前端编排值以兼容不同版本 DSL。
 */
public enum RiskRuleOperator {
    EQ("=="),
    NE("!="),
    GT(">"),
    GTE(">="),
    LT("<"),
    LTE("<="),
    LIKE("LIKE"),
    STARTS_WITH("STARTS_WITH"),
    ENDS_WITH("ENDS_WITH"),
    CONTAINS("CONTAINS"),
    IN("IN"),
    NOT_IN("NOT_IN"),
    INTERSECTS("INTERSECTS"),
    EXISTS("EXISTS"),
    IS_EMPTY("IS_EMPTY"),
    IS_NULL("IS_NULL"),
    BEFORE("BEFORE"),
    AFTER("AFTER"),
    BETWEEN_TIME("BETWEEN_TIME");

    private final String wireValue;

    /**
     * 解析操作人标识。
     *
     * @param wireValue 待处理值，用于规则计算或转换。
     */
    RiskRuleOperator(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * 返回 DSL 中使用的操作符字符串。
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * 按 DSL 字符串或枚举名解析操作符。
     */
    static Optional<RiskRuleOperator> fromWireValue(String value) {
        // 同时接受 == 等符号操作符和旧编排客户端输出的枚举名。
        return Arrays.stream(values())
                .filter(operator -> operator.name().equalsIgnoreCase(value)
                        || operator.wireValue.equalsIgnoreCase(value))
                .findFirst();
    }
}
