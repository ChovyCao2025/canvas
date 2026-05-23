package org.chovy.canvas.engine.request;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CanvasExecutionRequestPropertiesValidator {

    private final int dispatchBatchSize;
    private final long dispatchFixedDelayMs;
    private final long retryDelayMs;
    private final int maxAttempts;
    private final long runningStaleSeconds;
    private final long maxRetryDelayMs;
    private final long heartbeatIntervalMs;
    private final int perCanvasBatchLimit;

    public CanvasExecutionRequestPropertiesValidator(
            @Value("${canvas.execution-request.dispatch-batch-size:200}") int dispatchBatchSize,
            @Value("${canvas.execution-request.dispatch-fixed-delay-ms:1000}") long dispatchFixedDelayMs,
            @Value("${canvas.execution-request.retry-delay-ms:5000}") long retryDelayMs,
            @Value("${canvas.execution-request.max-attempts:5}") int maxAttempts,
            @Value("${canvas.execution-request.running-stale-sec:300}") long runningStaleSeconds,
            @Value("${canvas.execution-request.max-retry-delay-ms:60000}") long maxRetryDelayMs,
            @Value("${canvas.execution-request.heartbeat-interval-ms:60000}") long heartbeatIntervalMs,
            @Value("${canvas.execution-request.per-canvas-batch-limit:0}") int perCanvasBatchLimit) {
        this.dispatchBatchSize = dispatchBatchSize;
        this.dispatchFixedDelayMs = dispatchFixedDelayMs;
        this.retryDelayMs = retryDelayMs;
        this.maxAttempts = maxAttempts;
        this.runningStaleSeconds = runningStaleSeconds;
        this.maxRetryDelayMs = maxRetryDelayMs;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        this.perCanvasBatchLimit = perCanvasBatchLimit;
    }

    @PostConstruct
    public void validate() {
        requirePositive("dispatch-batch-size", dispatchBatchSize);
        requirePositive("dispatch-fixed-delay-ms", dispatchFixedDelayMs);
        requirePositive("retry-delay-ms", retryDelayMs);
        requirePositive("max-attempts", maxAttempts);
        requirePositive("running-stale-sec", runningStaleSeconds);
        requirePositive("max-retry-delay-ms", maxRetryDelayMs);
        requirePositive("heartbeat-interval-ms", heartbeatIntervalMs);

        if (maxRetryDelayMs < retryDelayMs) {
            throw new IllegalStateException(
                    "canvas.execution-request.max-retry-delay-ms must be greater than or equal to "
                            + "canvas.execution-request.retry-delay-ms");
        }
        long runningStaleMs = runningStaleSeconds * 1000L;
        if (heartbeatIntervalMs >= runningStaleMs) {
            throw new IllegalStateException(
                    "canvas.execution-request.heartbeat-interval-ms must be smaller than "
                            + "canvas.execution-request.running-stale-sec");
        }
        if (perCanvasBatchLimit < 0) {
            throw new IllegalStateException(
                    "canvas.execution-request.per-canvas-batch-limit must be greater than or equal to 0");
        }
    }

    private void requirePositive(String property, long value) {
        if (value <= 0) {
            throw new IllegalStateException("canvas.execution-request." + property + " must be greater than 0");
        }
    }
}
