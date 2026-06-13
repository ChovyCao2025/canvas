package org.chovy.canvas.risk.domain.governance;

import java.util.List;

import org.chovy.canvas.risk.api.RiskSceneView;

public interface RiskSceneRepository {

    List<RiskSceneView> listScenes(Long tenantId);

    void saveAll(List<RiskSceneView> scenes);
}
