package org.chovy.canvas.engine.expression;

import java.util.Map;

/**
 * Local contract for expression compilation and execution.
 *
 * <p>Implementations own vendor-specific sandboxing, timeout handling, result
 * size limits, and compile-cache coordination.
 */
public interface ExpressionEngine {

    void precompile(Long canvasId, String nodeId, String code);

    Map<String, Object> execute(Long canvasId, String nodeId, String code, Map<String, Object> variables)
            throws ExpressionException;

    Object evaluate(String expression, Map<String, Object> variables) throws ExpressionException;

    void evictCanvas(Long canvasId);

    class ExpressionException extends Exception {
        public ExpressionException(String message) {
            super(message);
        }

        public ExpressionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
