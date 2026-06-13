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

    BigDecimal decimalValue();

    String textValue();

    Object value();

    RiskRuleJsonNode get(String field);

    RiskRuleJsonNode path(String field);

    RiskRuleJsonNode get(int index);

    int size();

    List<RiskRuleJsonNode> elements();

    List<String> fieldNames();
}
