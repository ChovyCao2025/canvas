package org.chovy.canvas.domain.risk.modeling;

/**
 * 风控模型调用超时异常。
 */
public class RiskModelTimeoutException extends RuntimeException {

    /**
     * 创建模型超时异常。
     */
    public RiskModelTimeoutException(String modelKey) {
        super("Risk model timed out: " + modelKey);
    }
}
