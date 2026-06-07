package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.UserInputFormDO;
import org.chovy.canvas.dal.dataobject.UserInputResponseDO;
import org.chovy.canvas.dal.dataobject.UserInputResumeAuditDO;
import org.chovy.canvas.dal.mapper.UserInputFormMapper;
import org.chovy.canvas.dal.mapper.UserInputResponseMapper;
import org.chovy.canvas.dal.mapper.UserInputResumeAuditMapper;
import org.chovy.canvas.dto.canvas.UserInputSubmitReq;
import org.chovy.canvas.dto.canvas.UserInputSubmitResp;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserInputService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private final UserInputFormMapper formMapper;
    private final UserInputResponseMapper responseMapper;
    private final UserInputResumeAuditMapper auditMapper;
    private final ObjectMapper objectMapper;
    private final CanvasExecutionService executionService;
    private final Clock clock;

    @Autowired
    public UserInputService(UserInputFormMapper formMapper,
                            UserInputResponseMapper responseMapper,
                            UserInputResumeAuditMapper auditMapper,
                            ObjectMapper objectMapper,
                            @Lazy CanvasExecutionService executionService) {
        this(formMapper, responseMapper, auditMapper, objectMapper, executionService, Clock.systemDefaultZone());
    }

    UserInputService(UserInputFormMapper formMapper,
                     UserInputResponseMapper responseMapper,
                     UserInputResumeAuditMapper auditMapper,
                     ObjectMapper objectMapper,
                     CanvasExecutionService executionService,
                     Clock clock) {
        this.formMapper = formMapper;
        this.responseMapper = responseMapper;
        this.auditMapper = auditMapper;
        this.objectMapper = objectMapper;
        this.executionService = executionService;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public PendingInput createPending(ExecutionContext ctx,
                                      String nodeId,
                                      Object schema,
                                      String completedNodeId,
                                      String timeoutNodeId,
                                      LocalDateTime expiresAt) {
        requireRuntime(ctx, nodeId);
        String schemaJson = writeJson(schema);
        String idempotencyKey = idempotencyKey(ctx.getExecutionId(), nodeId, ctx.getUserId());
        UserInputResponseDO existing = responseMapper.selectOne(new LambdaQueryWrapper<UserInputResponseDO>()
                .eq(UserInputResponseDO::getTenantId, tenantId(ctx))
                .eq(UserInputResponseDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
        if (existing != null) {
            return new PendingInput(existing.getFormId(), existing.getId(), existing.getStatus(),
                    existing.getExpiresAt(), existing.getTimeoutNodeId());
        }

        LocalDateTime now = now();
        UserInputFormDO form = new UserInputFormDO();
        form.setTenantId(tenantId(ctx));
        form.setCanvasId(ctx.getCanvasId());
        form.setVersionId(ctx.getVersionId());
        form.setExecutionId(ctx.getExecutionId());
        form.setNodeId(nodeId);
        form.setUserId(ctx.getUserId());
        form.setSchemaJson(schemaJson);
        form.setCompletedNodeId(blankToNull(completedNodeId));
        form.setTimeoutNodeId(blankToNull(timeoutNodeId));
        form.setExpiresAt(expiresAt);
        form.setCreatedAt(now);
        form.setUpdatedAt(now);
        formMapper.insert(form);

        UserInputResponseDO response = new UserInputResponseDO();
        response.setTenantId(tenantId(ctx));
        response.setFormId(form.getId());
        response.setCanvasId(ctx.getCanvasId());
        response.setVersionId(ctx.getVersionId());
        response.setExecutionId(ctx.getExecutionId());
        response.setNodeId(nodeId);
        response.setUserId(ctx.getUserId());
        response.setStatus(STATUS_PENDING);
        response.setIdempotencyKey(idempotencyKey);
        response.setCompletedNodeId(blankToNull(completedNodeId));
        response.setTimeoutNodeId(blankToNull(timeoutNodeId));
        response.setExpiresAt(expiresAt);
        response.setCreatedAt(now);
        response.setUpdatedAt(now);
        responseMapper.insert(response);
        return new PendingInput(form.getId(), response.getId(), response.getStatus(), expiresAt, response.getTimeoutNodeId());
    }

    @Transactional(rollbackFor = Exception.class)
    public UserInputSubmitResp submit(Long responseId, UserInputSubmitReq req) {
        UserInputResponseDO current = requireResponse(responseId);
        if (!STATUS_PENDING.equals(current.getStatus())) {
            return new UserInputSubmitResp(current.getId(), current.getStatus(), true);
        }
        LocalDateTime now = now();
        String responseJson = writeJson(req == null ? Map.of() : req.response());
        UserInputResponseDO update = new UserInputResponseDO();
        update.setStatus(STATUS_COMPLETED);
        update.setResponseJson(responseJson);
        update.setUpdatedAt(now);
        int updated = responseMapper.update(update, new LambdaUpdateWrapper<UserInputResponseDO>()
                .eq(UserInputResponseDO::getId, responseId)
                .eq(UserInputResponseDO::getStatus, STATUS_PENDING));
        if (updated <= 0) {
            UserInputResponseDO latest = requireResponse(responseId);
            return new UserInputSubmitResp(latest.getId(), latest.getStatus(), true);
        }

        Map<String, Object> response = req == null || req.response() == null ? Map.of() : req.response();
        Map<String, Object> payload = resumePayload(current, STATUS_COMPLETED, response);
        writeAudit(current, STATUS_COMPLETED, payload);
        triggerResume(current, STATUS_COMPLETED, payload);
        return new UserInputSubmitResp(current.getId(), STATUS_COMPLETED, false);
    }

    public UserInputResponseDO requireResponse(Long responseId) {
        if (responseId == null) {
            throw new IllegalArgumentException("responseId is required");
        }
        UserInputResponseDO response = responseMapper.selectById(responseId);
        if (response == null) {
            throw new IllegalArgumentException("User input response not found: " + responseId);
        }
        return response;
    }

    private void triggerResume(UserInputResponseDO response, String status, Map<String, Object> payload) {
        String msgId = response.getExecutionId() + ":user-input:" + response.getId() + ":" + status;
        Mono<Map<String, Object>> resume = executionService.trigger(
                response.getCanvasId(),
                response.getUserId(),
                STATUS_EXPIRED.equals(status) ? TriggerType.WAIT_TIMEOUT : TriggerType.WAIT_RESUME,
                NodeType.USER_INPUT,
                response.getNodeId(),
                payload,
                msgId,
                false);
        resume.subscribe();
    }

    private Map<String, Object> resumePayload(UserInputResponseDO response,
                                              String status,
                                              Map<String, Object> inputResponse) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(MapFieldKeys.SOURCE_NODE_ID, response.getNodeId());
        payload.put(MapFieldKeys.WAIT_RESUME_STATUS, status);
        payload.put(MapFieldKeys.EXECUTION_ID, response.getExecutionId());
        payload.put("inputResponseId", response.getId());
        payload.put("inputResponse", inputResponse == null ? Map.of() : inputResponse);
        payload.put("completedNodeId", response.getCompletedNodeId());
        payload.put("timeoutNodeId", response.getTimeoutNodeId());
        return payload;
    }

    private void writeAudit(UserInputResponseDO response, String status, Map<String, Object> payload) {
        UserInputResumeAuditDO audit = new UserInputResumeAuditDO();
        audit.setTenantId(response.getTenantId());
        audit.setResponseId(response.getId());
        audit.setExecutionId(response.getExecutionId());
        audit.setNodeId(response.getNodeId());
        audit.setUserId(response.getUserId());
        audit.setResumeStatus(status);
        audit.setResumePayload(writeJson(payload));
        audit.setCreatedAt(now());
        auditMapper.insert(audit);
    }

    private void requireRuntime(ExecutionContext ctx, String nodeId) {
        if (ctx == null || ctx.getExecutionId() == null || ctx.getCanvasId() == null
                || ctx.getVersionId() == null || ctx.getUserId() == null || nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("User input runtime context is incomplete");
        }
    }

    private String idempotencyKey(String executionId, String nodeId, String userId) {
        return "USER_INPUT:" + executionId + ":" + nodeId + ":" + userId;
    }

    private Long tenantId(ExecutionContext ctx) {
        return ctx.getTenantId() == null ? 0L : ctx.getTenantId();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("User input JSON serialization failed", ex);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record PendingInput(Long formId, Long responseId, String status, LocalDateTime expiresAt, String timeoutNodeId) {
    }
}
