package org.chovy.canvas.domain.risk.runtime;

import org.chovy.canvas.domain.risk.dsl.RiskRuleOperand;

/**
 * 带请求上下文的风控特征解析器。
 */
@FunctionalInterface
public interface RiskRequestFeatureResolver {

    /**
     * 在指定决策请求上下文中解析规则操作数。
     */
    RiskResolvedValue resolve(RiskDecisionRequest request, RiskRuleOperand operand);
}
