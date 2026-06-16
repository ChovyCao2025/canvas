package org.chovy.canvas.risk.domain.dsl;

import java.math.BigDecimal;
import java.util.List;

/**
 * Minimal JSON tree view used by risk rule domain services.
 */
public interface RiskRuleJsonNode {

    boolean isMissing();

    boolean isNull();

    boolean isObject();

    boolean isArray();

    boolean isValue();

    boolean isBoolean();

    boolean isIntegralNumber();

    boolean isDecimalNumber();

    boolean isText();

    boolean booleanValue();

    long longValue();

    /**
     * 执行 decimalValue 相关的风控处理逻辑。
     */
    BigDecimal decimalValue();

    /**
     * 执行 textValue 相关的风控处理逻辑。
     */
    String textValue();

    /**
     * 执行 value 相关的风控处理逻辑。
     */
    Object value();

    /**
     * 执行 get 相关的风控处理逻辑。
     */
    RiskRuleJsonNode get(String field);

    /**
     * 执行 path 相关的风控处理逻辑。
     */
    RiskRuleJsonNode path(String field);

    /**
     * 执行 get 相关的风控处理逻辑。
     */
    RiskRuleJsonNode get(int index);

    int size();

    /**
     * 执行 elements 相关的风控处理逻辑。
     */
    List<RiskRuleJsonNode> elements();

    /**
     * 执行 fieldNames 相关的风控处理逻辑。
     */
    List<String> fieldNames();
}
