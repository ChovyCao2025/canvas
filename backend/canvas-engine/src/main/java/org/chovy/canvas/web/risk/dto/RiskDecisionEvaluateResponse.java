package org.chovy.canvas.web.risk.dto;

import java.util.List;

/**
 * 风控在线评估响应 DTO。
 *
 * @param requestId 请求幂等编号
 * @param decisionRunId 决策运行编号
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param strategyVersion 策略版本号
 * @param mode 运行模式
 * @param decision 决策动作
 * @param score 风险分
 * @param riskBand 风险带
 * @param reasons 原因列表
 * @param matchedRules 命中规则列表
 * @param labels 标签列表
 * @param missingFeatures 缺失特征列表
 * @param traceAvailable 是否可追踪
 * @param latencyMs 决策耗时
 */
public record RiskDecisionEvaluateResponse(
        String requestId,
        String decisionRunId,
        String sceneKey,
        String strategyKey,
        int strategyVersion,
        String mode,
        String decision,
        int score,
        String riskBand,
        List<String> reasons,
        List<String> matchedRules,
        List<String> labels,
        List<String> missingFeatures,
        boolean traceAvailable,
        int latencyMs
) {
}
