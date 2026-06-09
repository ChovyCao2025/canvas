package org.chovy.canvas.domain.risk.lab;

import org.chovy.canvas.domain.risk.runtime.RiskDecisionAction;

import java.util.Map;

/**
 * 风控仿真结果。
 *
 * @param simulationId 仿真编号
 * @param status 仿真状态
 * @param sampleSize 样本数量
 * @param actionDistribution 基线动作分布
 * @param changedActionCount 动作变化样本数
 * @param actionChanges 动作变化明细
 */
public record RiskSimulationResult(
        String simulationId,
        RiskSimulationStatus status,
        int sampleSize,
        Map<RiskDecisionAction, Integer> actionDistribution,
        int changedActionCount,
        Map<String, Integer> actionChanges
) {
}
