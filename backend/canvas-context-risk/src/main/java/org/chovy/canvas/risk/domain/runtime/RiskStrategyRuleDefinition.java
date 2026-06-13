package org.chovy.canvas.risk.domain.runtime;

import org.chovy.canvas.risk.domain.dsl.RiskRuntimeMode;

import java.util.List;

/**
 * 风控策略规则定义。
 *
 * @param ruleKey 规则键
 * @param priority 规则优先级
 * @param mode 规则运行模式
 * @param dslJson 规则 DSL JSON
 * @param action 命中动作
 * @param scoreDelta 分数增量
 * @param reasonCode 原因码
 * @param labels 规则标签
 */
public record RiskStrategyRuleDefinition(
        String ruleKey,
        int priority,
        RiskRuntimeMode mode,
        String dslJson,
        String action,
        int scoreDelta,
        String reasonCode,
        List<String> labels
) {

    public RiskStrategyRuleDefinition {
        labels = labels == null ? List.of() : List.copyOf(labels);
    }
}
