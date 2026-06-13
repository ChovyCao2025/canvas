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

@Repository
public class MyBatisExecutionTraceRepository implements ExecutionTraceRepository {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final CanvasExecutionMapper executionMapper;
    private final CanvasExecutionTraceMapper traceMapper;

    public MyBatisExecutionTraceRepository(CanvasExecutionMapper executionMapper, CanvasExecutionTraceMapper traceMapper) {
        this.executionMapper = executionMapper;
        this.traceMapper = traceMapper;
    }

    @Override
    public void saveStarted(ExecutionTraceRecord trace) {
        executionMapper.insert(toExecutionRow(trace));
    }

    @Override
    public void appendNode(ExecutionNodeTraceRecord nodeTrace) {
        traceMapper.insert(toTraceRow(nodeTrace));
    }

    @Override
    public void markFinished(Long tenantId, String executionId, String status, String failureReason, Instant finishedAt) {
        CanvasExecutionDO row = new CanvasExecutionDO();
        row.id = executionId;
        row.tenantId = tenantId;
        row.status = statusCode(status);
        row.result = failureReason == null || failureReason.isBlank() ? "{}" : "{\"error\":\"" + escape(failureReason) + "\"}";
        row.updatedAt = localDateTime(finishedAt);
        executionMapper.updateById(row);
    }

    @Override
    public ExecutionTraceView get(Long tenantId, String executionId) {
        CanvasExecutionDO execution = executionMapper.selectById(executionId);
        if (execution == null || !tenantId.equals(execution.tenantId)) {
            throw new IllegalStateException("execution trace not found: tenantId=" + tenantId
                    + ", executionId=" + executionId);
        }
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

    ExecutionTraceView.NodeResultView toNodeResultView(CanvasExecutionTraceDO row) {
        return new ExecutionTraceView.NodeResultView(
                row.nodeId,
                row.nodeType,
                nodeStatusName(row.status),
                row.errorMsg,
                parseJsonObject(row.outputData));
    }

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

    private int statusCode(String status) {
        return switch (status == null ? "" : status) {
            case "PAUSED", "WAITING" -> 1;
            case "SUCCESS", "RESUMED" -> 2;
            case "FAILED" -> 3;
            default -> 0;
        };
    }

    private int nodeStatusCode(String status) {
        return switch (status == null ? "" : status) {
            case "SUCCESS" -> 1;
            case "FAILED" -> 2;
            case "SKIPPED" -> 3;
            case "WAITING" -> 4;
            default -> 0;
        };
    }

    private String statusName(Integer status) {
        return switch (status == null ? 0 : status) {
            case 1 -> "PAUSED";
            case 2 -> "SUCCESS";
            case 3 -> "FAILED";
            default -> "RUNNING";
        };
    }

    private String nodeStatusName(Integer status) {
        return switch (status == null ? 0 : status) {
            case 1 -> "SUCCESS";
            case 2 -> "FAILED";
            case 3 -> "SKIPPED";
            case 4 -> "WAITING";
            default -> "RUNNING";
        };
    }

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

    private String failureReason(String resultJson) {
        Object error = parseJsonObject(resultJson).get("error");
        return error == null ? "" : String.valueOf(error);
    }

    private LocalDateTime localDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant instant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
