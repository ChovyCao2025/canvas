package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.AiUsageAuditDO;
import org.chovy.canvas.dal.mapper.AiUsageAuditMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
/**
 * AiUsageAuditService 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
 */
public class AiUsageAuditService {

    private final CopyOnWriteArrayList<AiUsageAuditEvent> events = new CopyOnWriteArrayList<>();
    private final AiUsageAuditMapper auditMapper;
    private final ObjectMapper objectMapper;

    public AiUsageAuditService() {
        this(null, new ObjectMapper());
    }

    @Autowired
    public AiUsageAuditService(AiUsageAuditMapper auditMapper, ObjectMapper objectMapper) {
        this.auditMapper = auditMapper;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param event event 参数，用于 record 流程中的校验、计算或对象转换。
     */
    public void record(AiUsageAuditEvent event) {
        events.add(event);
        if (auditMapper != null) {
            auditMapper.insert(toRow(event));
        }
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @return 返回符合条件的数据列表或视图。
     */
    public List<AiUsageAuditEvent> recent() {
        return List.copyOf(events);
    }

    /**
     * 组装 AI 用量审计持久化对象。
     */
    private AiUsageAuditDO toRow(AiUsageAuditEvent event) {
        AiUsageAuditDO row = new AiUsageAuditDO();
        row.setTenantId(event.tenantId());
        row.setCanvasId(event.canvasId());
        row.setExecutionId(event.executionId());
        row.setNodeId(event.nodeId());
        row.setProviderId(event.providerId());
        row.setTemplateId(event.templateId());
        row.setModelKey(event.modelKey());
        row.setStatus(event.status());
        row.setFallbackUsed(event.fallbackUsed() ? 1 : 0);
        row.setLatencyMs(event.latencyMs());
        row.setPromptTokens(event.promptTokens());
        row.setCompletionTokens(event.completionTokens());
        row.setOutputJson(writeJson(event.output()));
        row.setErrorCode(event.errorCode());
        row.setErrorMessage(event.errorMessage());
        row.setCreatedAt(toLocalDateTime(event.createdAt()));
        return row;
    }

    private String writeJson(JsonNode output) {
        if (output == null || output.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(output);
        } catch (Exception ex) {
            return "{\"error\":\"AI audit output serialization failed\"}";
        }
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant == null ? Instant.now() : instant, ZoneId.systemDefault());
    }

    /**
     * AiUsageAuditEvent 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record AiUsageAuditEvent(
            Instant createdAt,
            Long tenantId,
            Long canvasId,
            String executionId,
            String nodeId,
            Long providerId,
            Long templateId,
            String modelKey,
            String status,
            boolean fallbackUsed,
            long latencyMs,
            Integer promptTokens,
            Integer completionTokens,
            JsonNode output,
            String errorCode,
            String errorMessage) {
    }
}
