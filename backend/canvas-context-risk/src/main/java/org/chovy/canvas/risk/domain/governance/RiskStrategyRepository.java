package org.chovy.canvas.risk.domain.governance;

import java.util.List;

import org.chovy.canvas.risk.api.RiskStrategyView;

public interface RiskStrategyRepository {

    List<RiskStrategyView> listStrategies(Long tenantId, String sceneKey);
}
