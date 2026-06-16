package org.chovy.canvas.execution.adapter.persistence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.execution.api.trace.ExecutionTraceView;
import org.chovy.canvas.execution.application.ExecutionNodeTraceRecord;
import org.chovy.canvas.execution.application.ExecutionTraceRecord;
import org.chovy.canvas.execution.application.ExecutionTraceRepository;
import org.springframework.stereotype.Repository;

/**
 * 定义 MyBatisExecutionTraceRepository 的执行上下文数据结构或业务契约。
 */
@Repository
public class MyBatisExecutionTraceRepository implements ExecutionTraceRepository {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    /**
     * 保存 executionMapper 对应的状态或配置。
     */
    private final CanvasExecutionMapper executionMapper;

    /**
     * 保存 traceMapper 对应的状态或配置。
     */
    private final CanvasExecutionTraceMapper traceMapper;

    /**
     * 执行 MyBatisExecutionTraceRepository 对应的业务处理。
     * @param executionMapper executionMapper 参数
     * @param traceMapper traceMapper 参数
     */
    public MyBatisExecutionTraceRepository(CanvasExecutionMapper executionMapper, CanvasExecutionTraceMapper traceMapper) {
        this.executionMapper = executionMapper;
        this.traceMapper = traceMapper;
    }

    /**
     * 执行 saveStarted 对应的业务处理。
     * @param trace trace 参数
     */
    @Override
    public void saveStarted(ExecutionTraceRecord trace) {
        executionMapper.insert(toExecutionRow(trace));
    }

    /**
     * 执行 appendNode 对应的业务处理。
     * @param nodeTrace nodeTrace 参数
     */
    @Override
    public void appendNode(ExecutionNodeTraceRecord nodeTrace) {
        traceMapper.insert(toTraceRow(nodeTrace));
    }

    /**
     * 执行 markFinished 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @param status status 参数
     * @param failureReason failureReason 参数
     * @param finishedAt finishedAt 参数
     */
    @Override
    public void markFinished(Long tenantId, String executionId, String status, String failureReason, Instant finishedAt) {
        CanvasExecutionDO row = new CanvasExecutionDO();
        row.id = executionId;
        row.tenantId = tenantId;
        row.status = statusCode(status);
        // 只更新结束态需要的列，保留开始记录中已经持久化的画布与版本信息。
        row.result = failureReason == null || failureReason.isBlank() ? "{}" : "{\"error\":\"" + escape(failureReason) + "\"}";
        row.updatedAt = localDateTime(finishedAt);
        executionMapper.updateById(row);
    }

    /**
     * 执行 get 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param executionId executionId 参数
     * @return 处理后的结果
     */
    @Override
    public ExecutionTraceView get(Long tenantId, String executionId) {
        CanvasExecutionDO execution = executionMapper.selectById(executionId);
        if (execution == null || !tenantId.equals(execution.tenantId)) {
            throw new IllegalStateException("execution trace not found: tenantId=" + tenantId
                    + ", executionId=" + executionId);
        }
        // 轨迹节点历史可能来自旧数据，保留 tenantId 为空的兼容记录。
        List<ExecutionTraceView.NodeResultView> nodeResults = traceMapper.selectByExecutionId(executionId).stream()
                .filter(row -> tenantId.equals(row.tenantId) || row.tenantId == null)
                .map(this::toNodeResultView)
                .toList();
        return new ExecutionTraceView(
                execution.tenantId,
                execution.id,
                execution.canvasId,
                statusName(execution.status),
                instant(execution.createdAt),
                instant(execution.updatedAt),
                nodeResults,
                failureReason(execution.result));
    }

    /**
     * 执行 toExecutionRow 对应的业务处理。
     * @param trace trace 参数
     */
    CanvasExecutionDO toExecutionRow(ExecutionTraceRecord trace) {
        CanvasExecutionDO row = new CanvasExecutionDO();
        row.id = trace.executionId();
        row.tenantId = trace.tenantId();
        row.canvasId = trace.canvasId();
        row.versionId = trace.versionId();
        row.status = statusCode(trace.status());
        row.result = trace.failureReason().isBlank() ? "{}" : "{\"error\":\"" + escape(trace.failureReason()) + "\"}";
        row.createdAt = localDateTime(trace.startedAt());
        row.updatedAt = localDateTime(trace.finishedAt() == null ? trace.startedAt() : trace.finishedAt());
        return row;
    }

    /**
     * 执行 toTraceRow 对应的业务处理。
     * @param nodeTrace nodeTrace 参数
     */
    CanvasExecutionTraceDO toTraceRow(ExecutionNodeTraceRecord nodeTrace) {
        CanvasExecutionTraceDO row = new CanvasExecutionTraceDO();
        row.tenantId = nodeTrace.tenantId();
        row.executionId = nodeTrace.executionId();
        row.nodeId = nodeTrace.nodeId();
        row.nodeType = nodeTrace.nodeType();
        row.nodeName = nodeTrace.nodeId();
        row.status = nodeStatusCode(nodeTrace.status());
        row.outputData = jsonObject(nodeTrace.outputData());
        row.errorMsg = nodeTrace.error();
        row.startedAt = localDateTime(nodeTrace.occurredAt());
        row.finishedAt = localDateTime(nodeTrace.occurredAt());
        row.durationMs = 0L;
        return row;
    }

    /**
     * 执行 toNodeResultView 对应的业务处理。
     * @param row row 参数
     * @return 处理后的结果
     */
    ExecutionTraceView.NodeResultView toNodeResultView(CanvasExecutionTraceDO row) {
        return new ExecutionTraceView.NodeResultView(
                row.nodeId,
                row.nodeType,
                nodeStatusName(row.status),
                row.errorMsg,
                parseJsonObject(row.outputData));
    }

    /**
     * 执行 jsonObject 对应的业务处理。
     * @param values values 参数
     */
    private String jsonObject(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(values);
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to serialize execution trace output", e);
        }
    }

    /**
     * 执行 statusCode 对应的业务处理。
     * @param status status 参数
     */
    private int statusCode(String status) {
        return switch (status == null ? "" : status) {
            case "PAUSED", "WAITING" -> 1;
            case "SUCCESS", "RESUMED" -> 2;
            case "FAILED" -> 3;
            default -> 0;
        };
    }

    /**
     * 执行 nodeStatusCode 对应的业务处理。
     * @param status status 参数
     */
    private int nodeStatusCode(String status) {
        return switch (status == null ? "" : status) {
            case "SUCCESS" -> 1;
            case "FAILED" -> 2;
            case "SKIPPED" -> 3;
            case "WAITING" -> 4;
            default -> 0;
        };
    }

    /**
     * 执行 statusName 对应的业务处理。
     * @param status status 参数
     */
    private String statusName(Integer status) {
        return switch (status == null ? 0 : status) {
            case 1 -> "PAUSED";
            case 2 -> "SUCCESS";
            case 3 -> "FAILED";
            default -> "RUNNING";
        };
    }

    /**
     * 执行 nodeStatusName 对应的业务处理。
     * @param status status 参数
     */
    private String nodeStatusName(Integer status) {
        return switch (status == null ? 0 : status) {
            case 1 -> "SUCCESS";
            case 2 -> "FAILED";
            case 3 -> "SKIPPED";
            case 4 -> "WAITING";
            default -> "RUNNING";
        };
    }

    /**
     * 执行 parseJsonObject 对应的业务处理。
     * @param json json 参数
     * @return 处理后的结果
     */
    private Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }

    /**
     * 执行 failureReason 对应的业务处理。
     * @param resultJson resultJson 参数
     */
    private String failureReason(String resultJson) {
        Object error = parseJsonObject(resultJson).get("error");
        return error == null ? "" : String.valueOf(error);
    }

    /**
     * 执行 localDateTime 对应的业务处理。
     * @param instant instant 参数
     */
    private LocalDateTime localDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /**
     * 执行 instant 对应的业务处理。
     * @param dateTime dateTime 参数
     * @return 处理后的结果
     */
    private Instant instant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }

    /**
     * 执行 escape 对应的业务处理。
     * @param value value 参数
     * @return 处理后的结果
     */
    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
