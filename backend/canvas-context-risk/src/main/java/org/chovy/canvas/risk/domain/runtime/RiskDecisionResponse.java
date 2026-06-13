package org.chovy.canvas.risk.domain.runtime;

import org.chovy.canvas.risk.domain.dsl.RiskRuntimeMode;

import java.util.List;

/**
 * 对外风控决策结果，包含审计标签、命中规则引用和追踪可用性。
 *
 * @param requestId 请求幂等编号
 * @param decisionRunId 决策运行编号
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param strategyVersion 策略版本号
 * @param mode 运行模式
 * @param action 最终决策动作
 * @param score 最终风险分
 * @param riskBand 风险带
 * @param reasons 决策原因列表
 * @param matchedRules 命中规则引用列表
 * @param labels 决策标签列表
 * @param missingFeatures 缺失特征列表
 * @param latencyMs 决策耗时
 * @param traceAvailable 是否可追踪
 */
public record RiskDecisionResponse(
        String requestId,
        String decisionRunId,
        String sceneKey,
        String strategyKey,
        int strategyVersion,
        RiskRuntimeMode mode,
        RiskDecisionAction action,
        int score,
        RiskBand riskBand,
        List<String> reasons,
        List<String> matchedRules,
        List<String> labels,
        List<String> missingFeatures,
        int latencyMs,
        boolean traceAvailable
) {
}
