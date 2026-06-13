package org.chovy.canvas.risk.application;

import java.util.List;
import java.util.Objects;

import org.chovy.canvas.risk.api.RiskStrategyFacade;
import org.chovy.canvas.risk.api.RiskStrategyView;
import org.chovy.canvas.risk.domain.governance.RiskStrategyRepository;
import org.springframework.stereotype.Service;

@Service
public class RiskStrategyApplicationService implements RiskStrategyFacade {

    private final RiskStrategyRepository repository;

    public RiskStrategyApplicationService(RiskStrategyRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RiskStrategyView> listStrategies(Long tenantId, String sceneKey) {
        Objects.requireNonNull(tenantId, "tenantId");
        return repository.listStrategies(tenantId, sceneKey);
    }
}
