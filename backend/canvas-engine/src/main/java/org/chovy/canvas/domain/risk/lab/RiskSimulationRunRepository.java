package org.chovy.canvas.domain.risk.lab;

import java.util.List;

/**
 * 风控仿真运行仓储。
 */
public interface RiskSimulationRunRepository {

    /**
     * 保存仿真运行结果。
     */
    void save(RiskSimulationRequest request, RiskSimulationResult result);

    /**
     * 查询仿真历史。
     */
    List<RiskSimulationHistoryView> list(Long tenantId, String sceneKey, int limit);
}
