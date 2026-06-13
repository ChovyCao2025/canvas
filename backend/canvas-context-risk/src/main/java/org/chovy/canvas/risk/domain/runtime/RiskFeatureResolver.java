package org.chovy.canvas.risk.domain.runtime;

import org.chovy.canvas.risk.domain.dsl.RiskRuleOperand;

/**
 * 简化风控特征解析器接口，用于不依赖完整请求上下文的评估场景。
 */
@FunctionalInterface
public interface RiskFeatureResolver {

    /**
     * 解析规则操作数对应的运行时值。
     */
    RiskResolvedValue resolve(RiskRuleOperand operand);
}
