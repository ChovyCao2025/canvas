package org.chovy.canvas.domain.risk.lab;

import org.chovy.canvas.domain.risk.runtime.RiskDecisionAction;

import java.util.Map;

/**
 * 风控仿真历史视图。
 *
 * @param simulationId 仿真编号
 * @param sceneKey 场景业务键
 * @param strategyKey 策略业务键
 * @param baselineVersion 基线版本
 * @param candidateVersion 候选版本
 * @param status 仿真状态
 * @param sampleSize 样本数量
 * @param actionDistribution 动作分布
 * @param changedActionCount 动作变化样本数
 * @param actionChanges 动作变化明细
 * @param createdAt 创建时间
 */
public record RiskSimulationHistoryView(
        String simulationId,
        String sceneKey,
        String strategyKey,
        int baselineVersion,
        int candidateVersion,
        RiskSimulationStatus status,
        int sampleSize,
        Map<RiskDecisionAction, Integer> actionDistribution,
        int changedActionCount,
        Map<String, Integer> actionChanges,
        String createdAt
) {
}
