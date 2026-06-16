package org.chovy.canvas.execution.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.chovy.canvas.execution.api.ExecutionRerunFacade;

/**
 * 定义 ExecutionRerunCatalog 的执行上下文数据结构或业务契约。
 */
public class ExecutionRerunCatalog {

    /**
     * 保存 MODE_DRY_RUN 对应的状态或配置。
     */
    private static final String MODE_DRY_RUN = "DRY_RUN";

    /**
     * 保存 MODE_SKIP_SIDE_EFFECTS 对应的状态或配置。
     */
    private static final String MODE_SKIP_SIDE_EFFECTS = "SKIP_SIDE_EFFECTS";

    /**
     * 保存 MODE_ADMIN_REPLAY 对应的状态或配置。
     */
    private static final String MODE_ADMIN_REPLAY = "ADMIN_REPLAY";

    /**
     * 保存 STATUS_SUCCESS 对应的状态或配置。
     */
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final Map<Long, AuditState> audits = new LinkedHashMap<>();

    /**
     * 保存 nextAuditId 对应的状态或配置。
     */
    private long nextAuditId = 1L;

    public ExecutionRerunFacade.RerunResult rerun(Long tenantId, String operator, boolean admin, Long canvasId,
                                                  ExecutionRerunFacade.RerunCommand command) {
        ExecutionRerunFacade.RerunCommand body = command == null
                ? new ExecutionRerunFacade.RerunCommand(null, null, null, null, null, null, null)
                : command;
        String mode = normalizeMode(body.mode());
        String reason = requireReason(body.reason());
        requireReplayRole(mode, admin);
        String userId = requireText(body.userId(), "userId");
        Map<String, Object> inputParams = mergedInputParams(body.inputParams(), mode);

        Long auditId = nextAuditId++;
        AuditState audit = new AuditState(auditId, safeTenantId(tenantId), canvasId, userId, body.testUserId(),
                blankToNull(body.originalExecutionId()), mode, reason, actorOrDefault(operator), STATUS_SUCCESS,
                inputParams, timestamp(auditId), timestamp(auditId));
        audits.put(auditId, audit);

        Map<String, Object> execution = new LinkedHashMap<>();
        execution.put("canvasId", canvasId);
        execution.put("userId", userId);
        execution.put("mode", mode);
        execution.put("dryRun", !MODE_ADMIN_REPLAY.equals(mode));
        execution.put("dedupKey", "rerun-" + auditId);
        execution.put("inputParams", inputParams);
        if (body.graphJson() != null) {
            execution.put("graphJson", body.graphJson());
        }
        return new ExecutionRerunFacade.RerunResult(auditId, mode, STATUS_SUCCESS, execution);
    }

    /**
     * 执行 audit 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param id id 参数
     * @return 处理后的结果
     */
    public ExecutionRerunFacade.AuditRow audit(Long tenantId, Long id) {
        AuditState row = audits.get(id);
        if (row == null || !safeTenantId(tenantId).equals(row.tenantId)) {
            throw new IllegalArgumentException("rerun audit not found");
        }
        return row.toView();
    }

    /**
     * 执行 audits 对应的业务处理。
     * @param tenantId tenantId 参数
     * @param canvasId canvasId 参数
     * @return 处理后的结果
     */
    public List<ExecutionRerunFacade.AuditRow> audits(Long tenantId, Long canvasId) {
        Long normalizedTenantId = safeTenantId(tenantId);
        return audits.values().stream()
                .filter(row -> normalizedTenantId.equals(row.tenantId))
                .filter(row -> canvasId == null || canvasId.equals(row.canvasId))
                .sorted(Comparator.comparing(AuditState::createdAt).reversed())
                .limit(100)
                .map(AuditState::toView)
                .toList();
    }

    /**
     * 执行 mergedInputParams 对应的业务处理。
     * @param requestParams requestParams 参数
     * @param mode mode 参数
     * @return 处理后的结果
     */
    private static Map<String, Object> mergedInputParams(Map<String, Object> requestParams, String mode) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (requestParams != null) {
            merged.putAll(requestParams);
        }
        if (MODE_SKIP_SIDE_EFFECTS.equals(mode)) {
            merged.put("__skipSideEffects", true);
        }
        return Map.copyOf(merged);
    }

    /**
     * 执行 normalizeMode 对应的业务处理。
     * @param mode mode 参数
     */
    private static String normalizeMode(String mode) {
        String normalized = blankToNull(mode);
        if (normalized == null) {
            return MODE_DRY_RUN;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!MODE_DRY_RUN.equals(normalized)
                && !MODE_SKIP_SIDE_EFFECTS.equals(normalized)
                && !MODE_ADMIN_REPLAY.equals(normalized)) {
            throw new IllegalArgumentException("unsupported rerun mode: " + mode);
        }
        return normalized;
    }

    /**
     * 执行 requireReason 对应的业务处理。
     * @param reason reason 参数
     */
    private static String requireReason(String reason) {
        String value = requireText(reason, "reason");
        if (value.length() < 10) {
            throw new IllegalArgumentException("reason must be at least 10 characters");
        }
        return value;
    }

    /**
     * 执行 requireReplayRole 对应的业务处理。
     * @param mode mode 参数
     * @param admin admin 参数
     */
    private static void requireReplayRole(String mode, boolean admin) {
        if (MODE_ADMIN_REPLAY.equals(mode) && !admin) {
            throw new IllegalArgumentException("ADMIN_REPLAY requires admin role");
        }
    }

    /**
     * 执行 requireText 对应的业务处理。
     * @param value value 参数
     * @param field field 参数
     */
    private static String requireText(String value, String field) {
        String text = blankToNull(value);
        if (text == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return text;
    }

    /**
     * 执行 blankToNull 对应的业务处理。
     * @param value value 参数
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 执行 actorOrDefault 对应的业务处理。
     * @param operator operator 参数
     */
    private static String actorOrDefault(String operator) {
        return operator == null || operator.isBlank() ? "system" : operator.trim();
    }

    /**
     * 执行 safeTenantId 对应的业务处理。
     * @param tenantId tenantId 参数
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 执行 timestamp 对应的业务处理。
     * @param offsetSeconds offsetSeconds 参数
     * @return 处理后的结果
     */
    private static String timestamp(Long offsetSeconds) {
        return LocalDateTime.of(2026, 6, 14, 10, 0).plusSeconds(offsetSeconds).toString();
    }

    /**
     * 定义 AuditState 的执行上下文数据结构或业务契约。
     * @param id id 对应的数据字段
     * @param tenantId tenantId 对应的数据字段
     * @param canvasId canvasId 对应的数据字段
     * @param userId userId 对应的数据字段
     * @param testUserId testUserId 对应的数据字段
     * @param originalExecutionId originalExecutionId 对应的数据字段
     * @param mode mode 对应的数据字段
     * @param reason reason 对应的数据字段
     * @param operator operator 对应的数据字段
     * @param status status 对应的数据字段
     * @param inputParams inputParams 对应的数据字段
     * @param createdAt createdAt 对应的数据字段
     */
    private record AuditState(
            Long id,
            Long tenantId,
            Long canvasId,
            String userId,
            Long testUserId,
            String originalExecutionId,
            String mode,
            String reason,
            String operator,
            String status,
            Map<String, Object> inputParams,
            String createdAt,
            String updatedAt) {

        private ExecutionRerunFacade.AuditRow toView() {
            return new ExecutionRerunFacade.AuditRow(id, tenantId, canvasId, userId, testUserId,
                    originalExecutionId, mode, reason, operator, status, inputParams, createdAt, updatedAt);
        }
    }
}
