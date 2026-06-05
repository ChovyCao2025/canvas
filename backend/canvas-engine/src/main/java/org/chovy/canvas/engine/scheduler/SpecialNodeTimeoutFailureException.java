package org.chovy.canvas.engine.scheduler;

/** Fail-closed marker for special-node timeout terminal failures. */
public class SpecialNodeTimeoutFailureException extends IllegalStateException {

    public SpecialNodeTimeoutFailureException(String message) {
        super(message);
    }

    public SpecialNodeTimeoutFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public static boolean contains(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SpecialNodeTimeoutFailureException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
