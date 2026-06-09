package org.chovy.canvas.web.risk;

import java.util.List;

/**
 * 风控决策追踪读取器。
 */
public interface RiskDecisionTraceReader {

    /**
     * 查询租户最近决策追踪，可按场景过滤。
     */
    List<RiskDecisionTraceView> listTraces(Long tenantId, String sceneKey, int limit);
}
