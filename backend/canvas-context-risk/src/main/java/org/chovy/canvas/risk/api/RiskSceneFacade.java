package org.chovy.canvas.risk.api;

import java.util.List;

public interface RiskSceneFacade {

    List<RiskSceneView> listScenes(Long tenantId);
}
