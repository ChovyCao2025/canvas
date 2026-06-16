package org.chovy.canvas.risk.api;

import java.util.List;

/**
 * 定义 RiskSceneFacade 的风控模块职责和数据契约。
 */
public interface RiskSceneFacade {

    /**
     * 执行 listScenes 相关的风控处理逻辑。
     */
    List<RiskSceneView> listScenes(Long tenantId);
}
