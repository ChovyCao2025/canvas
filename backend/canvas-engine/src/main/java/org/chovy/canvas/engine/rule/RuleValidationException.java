package org.chovy.canvas.engine.rule;

/**
 * RuleValidationException 参与 engine.rule 场景的画布执行引擎处理。
 */
public class RuleValidationException extends RuntimeException {
    /**
     * 创建 RuleValidationException 实例并注入 engine.rule 场景依赖。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     */
    public RuleValidationException(String message) {
        super(message);
    }
}
