package org.chovy.canvas.risk.domain.dsl;

/**
 * Domain-facing JSON boundary for risk rule DSL parsing and canonicalization.
 */
public interface RiskRuleJsonCodec {

    RiskRuleJsonNode readTree(String json);

    String writeCanonical(Object value);
}
