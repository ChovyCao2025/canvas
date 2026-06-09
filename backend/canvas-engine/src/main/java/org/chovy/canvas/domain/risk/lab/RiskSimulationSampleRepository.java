package org.chovy.canvas.domain.risk.lab;

import org.chovy.canvas.domain.risk.runtime.RiskDecisionAction;
import org.chovy.canvas.domain.risk.runtime.RiskDecisionRunRecord;

import java.util.List;

/**
 * 风控仿真样本仓储。
 */
public interface RiskSimulationSampleRepository {

    /**
     * 查找指定租户和场景的历史决策样本。
     */
    List<RiskDecisionRunRecord> findSamples(Long tenantId, String sceneKey, int limit);

    /**
     * 在样本上评估候选策略动作。
     */
    RiskDecisionAction evaluateCandidate(RiskDecisionRunRecord sample, String strategyKey, int candidateVersion);
}
