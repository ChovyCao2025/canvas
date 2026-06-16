package org.chovy.canvas.execution.api.trace;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * 定义 TraceExplanationFacade 的执行上下文数据结构或业务契约。
 */
public class TraceExplanationFacade {

    /**
     * 保存 traceReader 对应的状态或配置。
     */
    private final TraceReader traceReader;

    /**
     * 执行 TraceExplanationFacade 对应的业务处理。
     * @param traceReader traceReader 参数
     */
    public TraceExplanationFacade(TraceReader traceReader) {
        this.traceReader = Objects.requireNonNull(traceReader, "traceReader is required");
    }

    /**
     * 执行 explain 对应的业务处理。
     * @param request request 参数
     * @return 处理后的结果
     */
    public TraceExplanation explain(TraceExplanationRequest request) {
        Objects.requireNonNull(request, "request is required");
        ExecutionTraceView trace = Objects.requireNonNull(
                traceReader.trace(new TraceLookupRequest(request.tenantId(), request.executionId())),
                "trace is required");
        Optional<ExecutionTraceView.NodeResultView> failedNode = trace.nodeResults().stream()
                .filter(TraceExplanationFacade::isFailure)
                .findFirst();
        // 优先使用失败节点错误；节点没有错误时再回退到执行级失败原因。
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

    /**
     * 执行 isFailure 对应的业务处理。
     * @param node node 参数
     */
    private static boolean isFailure(ExecutionTraceView.NodeResultView node) {
        String status = node.status().toUpperCase(Locale.ROOT);
        return "FAILED".equals(status) || "ERROR".equals(status) || !node.error().isBlank();
    }

    /**
     * 执行 classify 对应的业务处理。
     * @param error error 参数
     * @return 处理后的结果
     */
    private static String classify(String error) {
        String normalized = error == null ? "" : error.toLowerCase(Locale.ROOT);
        // 分类规则保持轻量级字符串匹配，避免解释接口依赖外部诊断服务。
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

    /**
     * 执行 recommendedActions 对应的业务处理。
     * @param errorType errorType 参数
     * @return 处理后的结果
     */
    private static List<String> recommendedActions(String errorType) {
        return switch (errorType) {
            case "TIMEOUT" -> List.of("Check provider latency, retry policy, and timeout configuration.");
            case "RATE_LIMIT" -> List.of("Reduce send concurrency or request a higher provider rate limit.");
            case "AUTHENTICATION" -> List.of("Verify provider credentials and tenant-level integration permissions.");
            case "VALIDATION" -> List.of("Inspect the failed node configuration and input payload.");
            default -> List.of("Review the execution trace, failed node output, and provider response.");
        };
    }

    /**
     * 定义 TraceReader 的执行上下文数据结构或业务契约。
     */
    @FunctionalInterface
    public interface TraceReader {
        /**
         * 执行 trace 对应的业务处理。
         * @param request request 参数
         * @return 处理后的结果
         */
        ExecutionTraceView trace(TraceLookupRequest request);
    }

    /**
     * 定义 TraceExplanationRequest 的执行上下文数据结构或业务契约。
     * @param tenantId tenantId 对应的数据字段
     * @param executionId executionId 对应的数据字段
     */
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

    /**
     * 定义 TraceLookupRequest 的执行上下文数据结构或业务契约。
     * @param tenantId tenantId 对应的数据字段
     * @param executionId executionId 对应的数据字段
     */
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

    /**
     * 定义 TraceExplanation 的执行上下文数据结构或业务契约。
     * @param executionId executionId 对应的数据字段
     * @param status status 对应的数据字段
     * @param failedNodeId failedNodeId 对应的数据字段
     * @param failedNodeType failedNodeType 对应的数据字段
     * @param errorType errorType 对应的数据字段
     * @param summary summary 对应的数据字段
     * @param recommendedActions recommendedActions 对应的数据字段
     */
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
