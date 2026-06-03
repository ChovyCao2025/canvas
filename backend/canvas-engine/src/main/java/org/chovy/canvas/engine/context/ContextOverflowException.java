package org.chovy.canvas.engine.context;

/**
 * Raised when an execution context cannot accept more node output within its
 * in-memory safety limit.
 */
public class ContextOverflowException extends RuntimeException {

    private final int currentSizeBytes;

    public ContextOverflowException(String message, int currentSizeBytes) {
        super(message);
        this.currentSizeBytes = currentSizeBytes;
    }

    public int getCurrentSizeBytes() {
        return currentSizeBytes;
    }
}
