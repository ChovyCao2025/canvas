package org.chovy.canvas.risk.domain.dsl;

/**
 * Domain-facing JSON boundary for risk rule DSL parsing and canonicalization.
 */
public interface RiskRuleJsonCodec {

    /**
     * 执行 readTree 相关的风控处理逻辑。
     */
    RiskRuleJsonNode readTree(String json);

    /**
     * 执行 writeCanonical 相关的风控处理逻辑。
     */
    String writeCanonical(Object value);
}
