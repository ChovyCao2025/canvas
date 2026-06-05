package org.chovy.canvas.engine.scheduler;

/** Fail-closed marker for incremental node state persistence failures. */
public class NodeStatePersistenceException extends IllegalStateException {

    public NodeStatePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public static boolean contains(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof NodeStatePersistenceException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
