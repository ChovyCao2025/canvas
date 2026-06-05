package org.chovy.canvas.engine.lifecycle;

/**
 * Raised when a new execution trigger is submitted after shutdown admission is closed.
 */
public class ExecutionLifecycleException extends IllegalStateException {

    public ExecutionLifecycleException(String source) {
        super("Execution lifecycle is stopping; rejecting new trigger work: " + source);
    }
}
