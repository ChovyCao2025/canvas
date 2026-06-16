package org.chovy.canvas.risk.domain.dsl;

import java.util.Arrays;
import java.util.Optional;

/**
 * 风控规则操作符，同时保留枚举名和前端编排值以兼容不同版本 DSL。
 */
public enum RiskRuleOperator {
    /**
     * 表示 EQ 枚举值。
     */
    EQ("=="),
    /**
     * 表示 NE 枚举值。
     */
    NE("!="),
    /**
     * 表示 GT 枚举值。
     */
    GT(">"),
    /**
     * 表示 GTE 枚举值。
     */
    GTE(">="),
    /**
     * 表示 LT 枚举值。
     */
    LT("<"),
    /**
     * 表示 LTE 枚举值。
     */
    LTE("<="),
    /**
     * 表示 LIKE 枚举值。
     */
    LIKE("LIKE"),
    /**
     * 表示 STARTS_WITH 枚举值。
     */
    STARTS_WITH("STARTS_WITH"),
    /**
     * 表示 ENDS_WITH 枚举值。
     */
    ENDS_WITH("ENDS_WITH"),
    /**
     * 表示 CONTAINS 枚举值。
     */
    CONTAINS("CONTAINS"),
    /**
     * 表示 IN 枚举值。
     */
    IN("IN"),
    /**
     * 表示 NOT_IN 枚举值。
     */
    NOT_IN("NOT_IN"),
    /**
     * 表示 INTERSECTS 枚举值。
     */
    INTERSECTS("INTERSECTS"),
    /**
     * 表示 EXISTS 枚举值。
     */
    EXISTS("EXISTS"),
    /**
     * 表示 IS_EMPTY 枚举值。
     */
    IS_EMPTY("IS_EMPTY"),
    /**
     * 表示 IS_NULL 枚举值。
     */
    IS_NULL("IS_NULL"),
    /**
     * 表示 BEFORE 枚举值。
     */
    BEFORE("BEFORE"),
    /**
     * 表示 AFTER 枚举值。
     */
    AFTER("AFTER"),
    /**
     * 表示 BETWEEN_TIME 枚举值。
     */
    BETWEEN_TIME("BETWEEN_TIME");

    /**
     * 保存 wireValue 对应的风控状态或配置。
     */
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
