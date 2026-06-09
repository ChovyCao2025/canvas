package org.chovy.canvas.web.risk;

import java.util.List;

/**
 * 风控决策追踪视图，供工作台读取最近决策和规则命中证据。
 *
 * @param traceId 决策运行编号
 * @param requestId 调用方幂等请求编号
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param strategyVersion 策略版本
 * @param mode 运行模式
 * @param decision 最终决策动作
 * @param score 风险分
 * @param riskBand 风险等级
 * @param latencyMs 决策耗时
 * @param createdAt 创建时间
 * @param matchedRules 命中规则列表
 */
public record RiskDecisionTraceView(
        String traceId,
        String requestId,
        String sceneKey,
        String strategyKey,
        Integer strategyVersion,
        String mode,
        String decision,
        Integer score,
        String riskBand,
        Integer latencyMs,
        String createdAt,
        List<String> matchedRules
) {
}
