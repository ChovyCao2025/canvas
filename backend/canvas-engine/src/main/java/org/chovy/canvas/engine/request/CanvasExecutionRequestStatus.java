package org.chovy.canvas.engine.request;

public final class CanvasExecutionRequestStatus {

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String RETRY = "RETRY";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String FAILED = "FAILED";

    private CanvasExecutionRequestStatus() {
    }
}
