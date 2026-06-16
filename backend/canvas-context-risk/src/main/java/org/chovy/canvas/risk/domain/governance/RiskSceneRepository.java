package org.chovy.canvas.risk.domain.governance;

import java.util.List;

import org.chovy.canvas.risk.api.RiskSceneView;

/**
 * 定义 RiskSceneRepository 的风控模块职责和数据契约。
 */
public interface RiskSceneRepository {

    /**
     * 执行 listScenes 相关的风控处理逻辑。
     */
    List<RiskSceneView> listScenes(Long tenantId);

    /**
     * 执行 saveAll 相关的风控处理逻辑。
     */
    void saveAll(List<RiskSceneView> scenes);
}
