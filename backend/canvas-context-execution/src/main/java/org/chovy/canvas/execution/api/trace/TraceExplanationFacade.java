package org.chovy.canvas.execution.api.trace;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class TraceExplanationFacade {

    private final TraceReader traceReader;

    public TraceExplanationFacade(TraceReader traceReader) {
        this.traceReader = Objects.requireNonNull(traceReader, "traceReader is required");
    }

    public TraceExplanation explain(TraceExplanationRequest request) {
        Objects.requireNonNull(request, "request is required");
        ExecutionTraceView trace = Objects.requireNonNull(
                traceReader.trace(new TraceLookupRequest(request.tenantId(), request.executionId())),
                "trace is required");
        Optional<ExecutionTraceView.NodeResultView> failedNode = trace.nodeResults().stream()
                .filter(TraceExplanationFacade::isFailure)
                .findFirst();
        String error = failedNode
                .map(ExecutionTraceView.NodeResultView::error)
                .filter(value -> !value.isBlank())
                .orElse(trace.failureReason());
        String errorType = classify(error);
        String failedNodeId = failedNode.map(ExecutionTraceView.NodeResultView::nodeId).orElse("");
        String failedNodeType = failedNode.map(ExecutionTraceView.NodeResultView::nodeType).orElse("");
        return new TraceExplanation(
                trace.executionId(),
                trace.status(),
                failedNodeId,
                failedNodeType,
                errorType,
                summary(trace, failedNodeId, failedNodeType, errorType, error),
                recommendedActions(errorType));
    }

    private static boolean isFailure(ExecutionTraceView.NodeResultView node) {
        String status = node.status().toUpperCase(Locale.ROOT);
        return "FAILED".equals(status) || "ERROR".equals(status) || !node.error().isBlank();
    }

    private static String classify(String error) {
        String normalized = error == null ? "" : error.toLowerCase(Locale.ROOT);
        if (normalized.contains("timeout") || normalized.contains("timed out")) {
            return "TIMEOUT";
        }
        if (normalized.contains("rate limit") || normalized.contains("429") || normalized.contains("throttle")) {
            return "RATE_LIMIT";
        }
        if (normalized.contains("unauthorized") || normalized.contains("forbidden") || normalized.contains("auth")) {
            return "AUTHENTICATION";
        }
        if (normalized.contains("validation") || normalized.contains("invalid") || normalized.contains("schema")) {
            return "VALIDATION";
        }
        return "UNKNOWN";
    }

    private static String summary(ExecutionTraceView trace,
                                  String failedNodeId,
                                  String failedNodeType,
                                  String errorType,
                                  String error) {
        String detail = error == null || error.isBlank() ? trace.failureReason() : error;
        if (!failedNodeId.isBlank()) {
            return "Node " + failedNodeId + " (" + failedNodeType + ") failed with "
                    + errorType + ": " + detail + ".";
        }
        return "Execution " + trace.executionId() + " failed with " + errorType + ": " + detail + ".";
    }

    private static List<String> recommendedActions(String errorType) {
        return switch (errorType) {
            case "TIMEOUT" -> List.of("Check provider latency, retry policy, and timeout configuration.");
            case "RATE_LIMIT" -> List.of("Reduce send concurrency or request a higher provider rate limit.");
            case "AUTHENTICATION" -> List.of("Verify provider credentials and tenant-level integration permissions.");
            case "VALIDATION" -> List.of("Inspect the failed node configuration and input payload.");
            default -> List.of("Review the execution trace, failed node output, and provider response.");
        };
    }

    @FunctionalInterface
    public interface TraceReader {
        ExecutionTraceView trace(TraceLookupRequest request);
    }

    public record TraceExplanationRequest(Long tenantId, String executionId) {

        public TraceExplanationRequest {
            if (tenantId == null || tenantId <= 0) {
                throw new IllegalArgumentException("tenantId is required");
            }
            if (executionId == null || executionId.isBlank()) {
                throw new IllegalArgumentException("executionId is required");
            }
        }
    }

    public record TraceLookupRequest(Long tenantId, String executionId) {

        public TraceLookupRequest {
            if (tenantId == null || tenantId <= 0) {
                throw new IllegalArgumentException("tenantId is required");
            }
            if (executionId == null || executionId.isBlank()) {
                throw new IllegalArgumentException("executionId is required");
            }
        }
    }

    public record TraceExplanation(
            String executionId,
            String status,
            String failedNodeId,
            String failedNodeType,
            String errorType,
            String summary,
            List<String> recommendedActions) {

        public TraceExplanation {
            if (executionId == null || executionId.isBlank()) {
                throw new IllegalArgumentException("executionId is required");
            }
            status = status == null || status.isBlank() ? "UNKNOWN" : status;
            failedNodeId = failedNodeId == null ? "" : failedNodeId;
            failedNodeType = failedNodeType == null ? "" : failedNodeType;
            errorType = errorType == null || errorType.isBlank() ? "UNKNOWN" : errorType;
            summary = summary == null ? "" : summary;
            recommendedActions = List.copyOf(recommendedActions == null ? List.of() : recommendedActions);
        }
    }
}
