package org.chovy.canvas.risk.api;

import java.util.List;

/**
 * 定义 RiskStrategyFacade 的风控模块职责和数据契约。
 */
public interface RiskStrategyFacade {

    /**
     * 执行 listStrategies 相关的风控处理逻辑。
     */
    List<RiskStrategyView> listStrategies(Long tenantId, String sceneKey);
}
