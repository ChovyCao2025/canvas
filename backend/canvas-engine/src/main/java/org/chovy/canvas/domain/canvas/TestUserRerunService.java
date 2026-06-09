package org.chovy.canvas.domain.canvas;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.ExecutionRerunAuditDO;
import org.chovy.canvas.dal.dataobject.TestUserDO;
import org.chovy.canvas.dal.dataobject.TestUserSetDO;
import org.chovy.canvas.dal.mapper.ExecutionRerunAuditMapper;
import org.chovy.canvas.dal.mapper.TestUserMapper;
import org.chovy.canvas.dal.mapper.TestUserSetMapper;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * TestUserRerunService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class TestUserRerunService {

    public static final String MODE_DRY_RUN = "DRY_RUN";
    public static final String MODE_SKIP_SIDE_EFFECTS = "SKIP_SIDE_EFFECTS";
    public static final String MODE_ADMIN_REPLAY = "ADMIN_REPLAY";
    public static final String STATUS_STARTED = "STARTED";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    private final TestUserSetMapper setMapper;
    private final TestUserMapper userMapper;
    private final ExecutionRerunAuditMapper auditMapper;
    private final CanvasExecutionService executionService;
    private final ObjectMapper objectMapper;

    /**
     * 初始化 TestUserRerunService 实例。
     *
     * @param setMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param userMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param executionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public TestUserRerunService(TestUserSetMapper setMapper,
                                TestUserMapper userMapper,
                                ExecutionRerunAuditMapper auditMapper,
                                CanvasExecutionService executionService,
                                ObjectMapper objectMapper) {
        this.setMapper = setMapper;
        this.userMapper = userMapper;
        this.auditMapper = auditMapper;
        this.executionService = executionService;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<TestUserSetDO> listSets(Long tenantId) {
        return setMapper.selectList(new LambdaQueryWrapper<TestUserSetDO>()
                .eq(TestUserSetDO::getTenantId, tenantId)
                .orderByDesc(TestUserSetDO::getCreatedAt));
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param req 请求对象，承载本次操作的输入参数。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    public TestUserSetDO createSet(Long tenantId, TestUserSetCreateReq req, String operator) {
        TestUserSetDO row = new TestUserSetDO();
        row.setTenantId(tenantId);
        row.setName(requireText(req.name(), "name"));
        row.setDescription(blankToNull(req.description()));
        row.setCreatedBy(blankToNull(operator));
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(row.getCreatedAt());
        setMapper.insert(row);
        return row;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param setId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<TestUserDO> listUsers(Long tenantId, Long setId) {
        return userMapper.selectList(new LambdaQueryWrapper<TestUserDO>()
                .eq(TestUserDO::getTenantId, tenantId)
                .eq(TestUserDO::getSetId, setId)
                .orderByDesc(TestUserDO::getCreatedAt));
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param setId 业务对象 ID，用于定位具体记录。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public TestUserDO createUser(Long tenantId, Long setId, TestUserCreateReq req) {
        TestUserDO row = new TestUserDO();
        row.setTenantId(tenantId);
        row.setSetId(setId);
        row.setUserId(requireText(req.userId(), "userId"));
        row.setDisplayName(blankToNull(req.displayName()));
        row.setProfileJson(toJson(req.profile()));
        row.setInputParams(toJson(req.inputParams()));
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(row.getCreatedAt());
        userMapper.insert(row);
        return row;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 getUser 流程生成的业务结果。
     */
    public TestUserDO getUser(Long tenantId, Long id) {
        TestUserDO row = userMapper.selectById(id);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("test user not found");
        }
        return row;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 preview 流程生成的业务结果。
     */
    public TestUserPreview preview(Long tenantId, Long id) {
        TestUserDO row = getUser(tenantId, id);
        Map<String, Object> profile = parseMap(row.getProfileJson());
        Map<String, Object> inputParams = parseMap(row.getInputParams());
        Map<String, Object> context = new HashMap<>(inputParams);
        context.put("userId", row.getUserId());
        context.put("profile", profile);
        return new TestUserPreview(row.getId(), row.getUserId(), row.getDisplayName(), profile, inputParams, context);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 rerun 流程生成的业务结果。
     */
    public Mono<RerunResult> rerun(Long tenantId, TenantContext context, Long canvasId, RerunRequest req) {
        String mode = normalizeMode(req.mode());
        String reason = requireReason(req.reason());
        requireReplayRole(mode, context);
        TestUserDO testUser = req.testUserId() == null ? null : getUser(tenantId, req.testUserId());
        String userId = blankToNull(req.userId());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (userId == null && testUser != null) {
            userId = testUser.getUserId();
        }
        userId = requireText(userId, "userId");
        Map<String, Object> inputParams = mergedInputParams(testUser, req.inputParams(), mode);

        ExecutionRerunAuditDO audit = new ExecutionRerunAuditDO();
        audit.setTenantId(tenantId);
        audit.setCanvasId(canvasId);
        audit.setUserId(userId);
        audit.setTestUserId(testUser == null ? null : testUser.getId());
        audit.setOriginalExecutionId(blankToNull(req.originalExecutionId()));
        audit.setMode(mode);
        audit.setReason(reason);
        audit.setOperator(context == null ? null : context.username());
        audit.setStatus(STATUS_STARTED);
        audit.setInputParams(toJson(inputParams));
        audit.setCreatedAt(LocalDateTime.now());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        audit.setUpdatedAt(audit.getCreatedAt());
        auditMapper.insert(audit);

        Mono<Map<String, Object>> execution = MODE_ADMIN_REPLAY.equals(mode)
                ? executionService.trigger(canvasId, userId, TriggerType.DIRECT_CALL, NodeType.DIRECT_CALL,
                        null, inputParams, "rerun-" + audit.getId(), false)
                : executionService.triggerDryRun(canvasId, userId, inputParams, req.graphJson());
        return execution
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(result -> {
                    markAudit(audit.getId(), STATUS_SUCCESS);
                    return new RerunResult(audit.getId(), mode, STATUS_SUCCESS, result);
                })
                .onErrorResume(error -> {
                    markAudit(audit.getId(), STATUS_FAILED);
                    return Mono.error(error);
                });
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 audit 流程生成的业务结果。
     */
    public ExecutionRerunAuditDO audit(Long tenantId, Long id) {
        ExecutionRerunAuditDO row = auditMapper.selectById(id);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("rerun audit not found");
        }
        return row;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param canvasId 业务对象 ID，用于定位具体记录。
     * @return 返回 audits 汇总后的集合、分页或映射视图。
     */
    public List<ExecutionRerunAuditDO> audits(Long tenantId, Long canvasId) {
        LambdaQueryWrapper<ExecutionRerunAuditDO> wrapper = new LambdaQueryWrapper<ExecutionRerunAuditDO>()
                .eq(ExecutionRerunAuditDO::getTenantId, tenantId)
                .orderByDesc(ExecutionRerunAuditDO::getCreatedAt)
                .last("LIMIT 100");
        if (canvasId != null) {
            wrapper.eq(ExecutionRerunAuditDO::getCanvasId, canvasId);
        }
        return auditMapper.selectList(wrapper);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param testUser test user 参数，用于 mergedInputParams 流程中的校验、计算或对象转换。
     * @param MapString map string 参数，用于 mergedInputParams 流程中的校验、计算或对象转换。
     * @param requestParams request params 参数，用于 mergedInputParams 流程中的校验、计算或对象转换。
     * @param mode mode 参数，用于 mergedInputParams 流程中的校验、计算或对象转换。
     * @return 返回 mergedInputParams 流程生成的业务结果。
     */
    private Map<String, Object> mergedInputParams(TestUserDO testUser, Map<String, Object> requestParams, String mode) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> merged = new HashMap<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (testUser != null) {
            merged.putAll(parseMap(testUser.getInputParams()));
            merged.put("profile", parseMap(testUser.getProfileJson()));
        }
        if (requestParams != null) {
            merged.putAll(requestParams);
        }
        if (MODE_SKIP_SIDE_EFFECTS.equals(mode)) {
            merged.put("__skipSideEffects", true);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return merged;
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @param status 业务状态，用于筛选或推进状态流转。
     */
    private void markAudit(Long id, String status) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ExecutionRerunAuditDO update = new ExecutionRerunAuditDO();
        update.setId(id);
        update.setStatus(status);
        update.setUpdatedAt(LocalDateTime.now());
        auditMapper.updateById(update);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param mode mode 参数，用于 requireReplayRole 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     */
    private void requireReplayRole(String mode, TenantContext context) {
        if (!MODE_ADMIN_REPLAY.equals(mode)) {
            return;
        }
        if (context == null || (!context.isSuperAdmin() && !context.isTenantAdmin())) {
            throw new IllegalArgumentException("ADMIN_REPLAY requires admin role");
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param mode mode 参数，用于 normalizeMode 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeMode(String mode) {
        String normalized = blankToNull(mode);
        if (normalized == null) {
            return MODE_DRY_RUN;
        }
        normalized = normalized.toUpperCase();
        if (!MODE_DRY_RUN.equals(normalized)
                && !MODE_SKIP_SIDE_EFFECTS.equals(normalized)
                && !MODE_ADMIN_REPLAY.equals(normalized)) {
            throw new IllegalArgumentException("unsupported rerun mode: " + mode);
        }
        return normalized;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param reason 原因说明，用于记录状态变化的业务依据。
     * @return 返回 require reason 生成的文本或业务键。
     */
    private String requireReason(String reason) {
        String value = requireText(reason, "reason");
        if (value.length() < 10) {
            throw new IllegalArgumentException("reason must be at least 10 characters");
        }
        return value;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 require text 生成的文本或业务键。
     */
    private String requireText(String value, String field) {
        String text = blankToNull(value);
        if (text == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return text;
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
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("json serialization failed", e);
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * TestUserSetCreateReq 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record TestUserSetCreateReq(String name, String description) {
    }

    /**
     * TestUserCreateReq 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record TestUserCreateReq(String userId, String displayName,
                                    Map<String, Object> profile,
                                    Map<String, Object> inputParams) {
    }

    /**
     * TestUserPreview 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record TestUserPreview(Long id, String userId, String displayName,
                                  Map<String, Object> profile,
                                  Map<String, Object> inputParams,
                                  Map<String, Object> context) {
    }

    /**
     * RerunRequest 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RerunRequest(String userId,
                               Long testUserId,
                               String originalExecutionId,
                               String mode,
                               String reason,
                               Map<String, Object> inputParams,
                               String graphJson) {
    }

    /**
     * RerunResult 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record RerunResult(Long auditId, String mode, String status, Map<String, Object> result) {
    }
}
