package org.chovy.canvas.risk.domain.runtime;

/**
 * 风控决策幂等回放异常，表示同一请求编号对应的事实载荷发生变化。
 */
public class RiskDecisionReplayMismatchException extends RuntimeException {

    /**
     * 创建回放不一致异常。
     */
    public RiskDecisionReplayMismatchException(String requestId) {
        super("risk decision request replay mismatch: " + requestId);
    }
}
