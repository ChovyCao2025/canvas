package org.chovy.canvas.risk.api;

public interface RiskDecisionFacade {

    RiskDecisionView evaluate(RiskDecisionCommand command);
}
