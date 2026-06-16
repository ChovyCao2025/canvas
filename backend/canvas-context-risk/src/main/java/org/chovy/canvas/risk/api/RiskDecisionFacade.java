package org.chovy.canvas.risk.api;

/**
 * 定义 RiskDecisionFacade 的风控模块职责和数据契约。
 */
public interface RiskDecisionFacade {

    /**
     * 执行 evaluate 相关的风控处理逻辑。
     */
    RiskDecisionView evaluate(RiskDecisionCommand command);
}
