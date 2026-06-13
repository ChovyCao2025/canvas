package org.chovy.canvas.risk.application;

import org.chovy.canvas.risk.api.RiskDecisionCommand;
import org.chovy.canvas.risk.api.RiskDecisionFacade;
import org.chovy.canvas.risk.api.RiskDecisionView;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionRequest;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionResponse;
import org.chovy.canvas.risk.domain.runtime.RiskDecisionService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class RiskDecisionApplicationService implements RiskDecisionFacade {

    private final RiskDecisionService decisionService;

    public RiskDecisionApplicationService(RiskDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @Override
    public RiskDecisionView evaluate(RiskDecisionCommand command) {
        Objects.requireNonNull(command, "command");
        RiskDecisionResponse response = decisionService.evaluate(new RiskDecisionRequest(
                command.tenantId(),
                command.requestId(),
                command.sceneKey(),
                command.eventTime(),
                command.event(),
                command.subject(),
                command.context(),
                command.features(),
                command.deadlineMs()));
        return new RiskDecisionView(
                response.requestId(),
                response.decisionRunId(),
                response.sceneKey(),
                response.strategyKey(),
                response.strategyVersion(),
                response.mode() == null ? null : response.mode().name(),
                response.action().name(),
                response.score(),
                response.riskBand().name(),
                response.reasons(),
                response.matchedRules(),
                response.labels(),
                response.missingFeatures(),
                response.traceAvailable(),
                response.latencyMs());
    }
}
