package org.chovy.canvas.risk.domain.governance;

import java.util.List;

import org.chovy.canvas.risk.api.RiskStrategyView;

/**
 * 定义 RiskStrategyRepository 的风控模块职责和数据契约。
 */
public interface RiskStrategyRepository {

    /**
     * 执行 listStrategies 相关的风控处理逻辑。
     */
    List<RiskStrategyView> listStrategies(Long tenantId, String sceneKey);
}
