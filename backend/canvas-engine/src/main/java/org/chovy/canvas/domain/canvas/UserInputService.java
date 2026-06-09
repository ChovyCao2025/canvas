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
/**
 * UserInputService 承载对应领域的业务规则、流程编排和结果转换。
 */
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
    /**
     * 初始化 UserInputService 实例。
     *
     * @param formMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param responseMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param executionService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public UserInputService(UserInputFormMapper formMapper,
                            UserInputResponseMapper responseMapper,
                            UserInputResumeAuditMapper auditMapper,
                            ObjectMapper objectMapper,
                            @Lazy CanvasExecutionService executionService) {
        this(formMapper, responseMapper, auditMapper, objectMapper, executionService, Clock.systemDefaultZone());
    }

    /**
     * 初始化 UserInputService 实例。
     *
     * @param formMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param responseMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param executionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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
    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param ctx ctx 参数，用于 createPending 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param schema schema 参数，用于 createPending 流程中的校验、计算或对象转换。
     * @param completedNodeId 业务对象 ID，用于定位具体记录。
     * @param timeoutNodeId 业务对象 ID，用于定位具体记录。
     * @param expiresAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    public PendingInput createPending(ExecutionContext ctx,
                                      String nodeId,
                                      Object schema,
                                      String completedNodeId,
                                      String timeoutNodeId,
                                      LocalDateTime expiresAt) {
        requireRuntime(ctx, nodeId);
        String schemaJson = writeJson(schema);
        String idempotencyKey = idempotencyKey(ctx.getExecutionId(), nodeId, ctx.getUserId());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        UserInputResponseDO existing = responseMapper.selectOne(new LambdaQueryWrapper<UserInputResponseDO>()
                .eq(UserInputResponseDO::getTenantId, tenantId(ctx))
                .eq(UserInputResponseDO::getIdempotencyKey, idempotencyKey)
                .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new PendingInput(form.getId(), response.getId(), response.getStatus(), expiresAt, response.getTimeoutNodeId());
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param responseId 业务对象 ID，用于定位具体记录。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 submit 流程生成的业务结果。
     */
    public UserInputSubmitResp submit(Long responseId, UserInputSubmitReq req) {
        UserInputResponseDO current = requireResponse(responseId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!STATUS_PENDING.equals(current.getStatus())) {
            return new UserInputSubmitResp(current.getId(), current.getStatus(), true);
        }
        LocalDateTime now = now();
        String responseJson = writeJson(req == null ? Map.of() : req.response());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new UserInputSubmitResp(current.getId(), STATUS_COMPLETED, false);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param responseId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireResponse 流程生成的业务结果。
     */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param response response 参数，用于 triggerResume 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param MapString map string 参数，用于 triggerResume 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param response response 参数，用于 resumePayload 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param inputResponse input response 参数，用于 resumePayload 流程中的校验、计算或对象转换。
     * @return 返回 resumePayload 流程生成的业务结果。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param response response 参数，用于 writeAudit 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param MapString map string 参数，用于 writeAudit 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param ctx ctx 参数，用于 requireRuntime 流程中的校验、计算或对象转换。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     */
    private void requireRuntime(ExecutionContext ctx, String nodeId) {
        if (ctx == null || ctx.getExecutionId() == null || ctx.getCanvasId() == null
                || ctx.getVersionId() == null || ctx.getUserId() == null || nodeId == null || nodeId.isBlank()) {
            throw new IllegalArgumentException("User input runtime context is incomplete");
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param executionId 业务对象 ID，用于定位具体记录。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @return 返回 idempotency key 生成的文本或业务键。
     */
    private String idempotencyKey(String executionId, String nodeId, String userId) {
        return "USER_INPUT:" + executionId + ":" + nodeId + ":" + userId;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param ctx ctx 参数，用于 tenantId 流程中的校验、计算或对象转换。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(ExecutionContext ctx) {
        return ctx.getTenantId() == null ? 0L : ctx.getTenantId();
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 write json 生成的文本或业务键。
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("User input JSON serialization failed", ex);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * PendingInput 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PendingInput(Long formId, Long responseId, String status, LocalDateTime expiresAt, String timeoutNodeId) {
    }
}
