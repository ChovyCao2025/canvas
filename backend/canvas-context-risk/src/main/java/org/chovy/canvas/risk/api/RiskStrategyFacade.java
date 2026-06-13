package org.chovy.canvas.risk.api;

import java.util.List;

public interface RiskStrategyFacade {

    List<RiskStrategyView> listStrategies(Long tenantId, String sceneKey);
}
