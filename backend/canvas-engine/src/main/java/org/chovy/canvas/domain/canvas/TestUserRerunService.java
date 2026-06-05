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

    public List<TestUserSetDO> listSets(Long tenantId) {
        return setMapper.selectList(new LambdaQueryWrapper<TestUserSetDO>()
                .eq(TestUserSetDO::getTenantId, tenantId)
                .orderByDesc(TestUserSetDO::getCreatedAt));
    }

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

    public List<TestUserDO> listUsers(Long tenantId, Long setId) {
        return userMapper.selectList(new LambdaQueryWrapper<TestUserDO>()
                .eq(TestUserDO::getTenantId, tenantId)
                .eq(TestUserDO::getSetId, setId)
                .orderByDesc(TestUserDO::getCreatedAt));
    }

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

    public TestUserDO getUser(Long tenantId, Long id) {
        TestUserDO row = userMapper.selectById(id);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("test user not found");
        }
        return row;
    }

    public TestUserPreview preview(Long tenantId, Long id) {
        TestUserDO row = getUser(tenantId, id);
        Map<String, Object> profile = parseMap(row.getProfileJson());
        Map<String, Object> inputParams = parseMap(row.getInputParams());
        Map<String, Object> context = new HashMap<>(inputParams);
        context.put("userId", row.getUserId());
        context.put("profile", profile);
        return new TestUserPreview(row.getId(), row.getUserId(), row.getDisplayName(), profile, inputParams, context);
    }

    public Mono<RerunResult> rerun(Long tenantId, TenantContext context, Long canvasId, RerunRequest req) {
        String mode = normalizeMode(req.mode());
        String reason = requireReason(req.reason());
        requireReplayRole(mode, context);
        TestUserDO testUser = req.testUserId() == null ? null : getUser(tenantId, req.testUserId());
        String userId = blankToNull(req.userId());
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
        audit.setUpdatedAt(audit.getCreatedAt());
        auditMapper.insert(audit);

        Mono<Map<String, Object>> execution = MODE_ADMIN_REPLAY.equals(mode)
                ? executionService.trigger(canvasId, userId, TriggerType.DIRECT_CALL, NodeType.DIRECT_CALL,
                        null, inputParams, "rerun-" + audit.getId(), false)
                : executionService.triggerDryRun(canvasId, userId, inputParams, req.graphJson());
        return execution
                .map(result -> {
                    markAudit(audit.getId(), STATUS_SUCCESS);
                    return new RerunResult(audit.getId(), mode, STATUS_SUCCESS, result);
                })
                .onErrorResume(error -> {
                    markAudit(audit.getId(), STATUS_FAILED);
                    return Mono.error(error);
                });
    }

    public ExecutionRerunAuditDO audit(Long tenantId, Long id) {
        ExecutionRerunAuditDO row = auditMapper.selectById(id);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("rerun audit not found");
        }
        return row;
    }

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

    private Map<String, Object> mergedInputParams(TestUserDO testUser, Map<String, Object> requestParams, String mode) {
        Map<String, Object> merged = new HashMap<>();
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
        return merged;
    }

    private void markAudit(Long id, String status) {
        ExecutionRerunAuditDO update = new ExecutionRerunAuditDO();
        update.setId(id);
        update.setStatus(status);
        update.setUpdatedAt(LocalDateTime.now());
        auditMapper.updateById(update);
    }

    private void requireReplayRole(String mode, TenantContext context) {
        if (!MODE_ADMIN_REPLAY.equals(mode)) {
            return;
        }
        if (context == null || (!context.isSuperAdmin() && !context.isTenantAdmin())) {
            throw new IllegalArgumentException("ADMIN_REPLAY requires admin role");
        }
    }

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

    private String requireReason(String reason) {
        String value = requireText(reason, "reason");
        if (value.length() < 10) {
            throw new IllegalArgumentException("reason must be at least 10 characters");
        }
        return value;
    }

    private String requireText(String value, String field) {
        String text = blankToNull(value);
        if (text == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return text;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

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

    public record TestUserSetCreateReq(String name, String description) {
    }

    public record TestUserCreateReq(String userId, String displayName,
                                    Map<String, Object> profile,
                                    Map<String, Object> inputParams) {
    }

    public record TestUserPreview(Long id, String userId, String displayName,
                                  Map<String, Object> profile,
                                  Map<String, Object> inputParams,
                                  Map<String, Object> context) {
    }

    public record RerunRequest(String userId,
                               Long testUserId,
                               String originalExecutionId,
                               String mode,
                               String reason,
                               Map<String, Object> inputParams,
                               String graphJson) {
    }

    public record RerunResult(Long auditId, String mode, String status, Map<String, Object> result) {
    }
}
