package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维护 CdpWarehousePrivacy 的内存目录和查询视图。
 */
public class CdpWarehousePrivacyCatalog {

    /**
     * 创建Erasure Request。
     */
    public Map<String, Object> createErasureRequest(Long tenantId, Map<String, Object> payload, String actor) {
        String subjectValue = requiredString(payload, "subjectValue");
        return values(
                "requestId", 1001L,
                "tenantId", tenantId,
                "subjectType", stringValue(payload, "subjectType", "USER_ID"),
                "subjectValue", subjectValue,
                "reason", stringValue(payload, "reason", "privacy_request"),
                "status", "REQUESTED",
                "createdBy", actor,
                "createdAt", now());
    }

    /**
     * 执行 recordAssetProof 对应的 CDP 业务操作。
     */
    public Map<String, Object> recordAssetProof(Long tenantId, Long requestId, Map<String, Object> payload,
                                                String actor) {
        return values(
                "proofId", 1101L,
                "requestId", requestId,
                "tenantId", tenantId,
                "assetKey", stringValue(payload, "assetKey", "profile"),
                "status", "RECORDED",
                "recordedBy", actor,
                "recordedAt", now());
    }

    /**
     * 执行 executeErasure 对应的 CDP 业务操作。
     */
    public Map<String, Object> executeErasure(Long tenantId, Long requestId, Map<String, Object> payload, String actor) {
        return values(
                "executionId", 1201L,
                "requestId", requestId,
                "tenantId", tenantId,
                "mode", stringValue(payload, "mode", "execute"),
                "status", "SUCCEEDED",
                "executedBy", actor,
                "executedAt", now());
    }

    /**
     * 执行 rebuildAudienceBitmaps 对应的 CDP 业务操作。
     */
    public Map<String, Object> rebuildAudienceBitmaps(Long tenantId, Long requestId, Map<String, Object> payload,
                                                      String actor) {
        return values(
                "rebuildId", 1301L,
                "requestId", requestId,
                "tenantId", tenantId,
                "audienceId", longValue(payload, "audienceId", 88L),
                "status", "QUEUED",
                "requestedBy", actor,
                "queuedAt", now());
    }

    /**
     * 执行 runAudienceRebuildAutomation 对应的 CDP 业务操作。
     */
    public Map<String, Object> runAudienceRebuildAutomation(Long tenantId, Map<String, Object> payload, String actor) {
        return automationRun(tenantId, actor, stringValue(payload, "strategy", "recent"));
    }

    /**
     * 查询Audience Rebuild Automation Runs列表。
     */
    public List<Map<String, Object>> listAudienceRebuildAutomationRuns(Long tenantId, int limit) {
        return List.of(automationRun(tenantId, "operator-1", "recent"));
    }

    /**
     * 返回audience Rebuild Automation Run。
     */
    public Map<String, Object> getAudienceRebuildAutomationRun(Long tenantId, Long runId) {
        Map<String, Object> run = automationRun(tenantId, "operator-1", "recent");
        run.put("runId", runId);
        return run;
    }

    /**
     * 执行 recentErasureRequests 对应的 CDP 业务操作。
     */
    public List<Map<String, Object>> recentErasureRequests(Long tenantId, String status, int limit) {
        return List.of(values(
                "requestId", 1001L,
                "tenantId", tenantId,
                "subjectType", "USER_ID",
                "subjectValue", "user-1",
                "status", blankToDefault(status, "REQUESTED"),
                "limit", limit));
    }

    /**
     * 返回erasure Request。
     */
    public Map<String, Object> getErasureRequest(Long tenantId, Long requestId) {
        return values(
                "requestId", requestId,
                "tenantId", tenantId,
                "subjectType", "USER_ID",
                "subjectValue", "user-1",
                "status", "REQUESTED");
    }

    /**
     * 执行 erasureSummary 对应的 CDP 业务操作。
     */
    public Map<String, Object> erasureSummary(Long tenantId) {
        return values(
                "tenantId", tenantId,
                "openCount", 1,
                "executedCount", 1,
                "tombstoneCount", 1,
                "asOf", now());
    }

    /**
     * 创建Tombstone。
     */
    public Map<String, Object> createTombstone(Long tenantId, Map<String, Object> payload, String actor) {
        return tombstone(tenantId, 2001L, null, "ACTIVE", payload, actor);
    }

    /**
     * 创建Tombstone From Erasure Request。
     */
    public Map<String, Object> createTombstoneFromErasureRequest(Long tenantId, Map<String, Object> payload,
                                                                 String actor) {
        return tombstone(tenantId, 2002L, longValue(payload, "requestId", 1001L), "ACTIVE", payload, actor);
    }

    /**
     * 执行 revokeTombstone 对应的 CDP 业务操作。
     */
    public Map<String, Object> revokeTombstone(Long tenantId, Long tombstoneId, Map<String, Object> payload,
                                               String actor) {
        Map<String, Object> tombstone = tombstone(tenantId, tombstoneId, null, "REVOKED", payload, actor);
        tombstone.put("reason", stringValue(payload, "reason", "revoked"));
        tombstone.put("revokedBy", actor);
        return tombstone;
    }

    /**
     * 查询Tombstones列表。
     */
    public List<Map<String, Object>> listTombstones(Long tenantId, String status, int limit) {
        return List.of(tombstone(tenantId, 2001L, null, blankToDefault(status, "ACTIVE"),
                Map.of("subjectValue", "user-1"), "operator-1"));
    }

    /**
     * 转换为mbstone Decision。
     */
    public Map<String, Object> tombstoneDecision(Long tenantId, String subjectType, String subjectValue) {
        String scopedSubjectValue = requiredText(subjectValue, "subjectValue");
        return values(
                "tenantId", tenantId,
                "subjectType", blankToDefault(subjectType, "USER_ID"),
                "subjectValue", scopedSubjectValue,
                "blocked", true,
                "matchedTombstoneId", 2001L,
                "decisionAt", now());
    }

    /**
     * 执行 automationRun 对应的 CDP 业务操作。
     */
    private static Map<String, Object> automationRun(Long tenantId, String actor, String strategy) {
        return values(
                "runId", 1401L,
                "tenantId", tenantId,
                "strategy", strategy,
                "status", "COMPLETED",
                "triggeredBy", actor,
                "startedAt", now(),
                "completedAt", now());
    }

    /**
     * 转换为mbstone。
     */
    private static Map<String, Object> tombstone(Long tenantId, Long tombstoneId, Long requestId, String status,
                                                 Map<String, Object> payload, String actor) {
        return values(
                "tombstoneId", tombstoneId,
                "tenantId", tenantId,
                "requestId", requestId,
                "subjectType", stringValue(payload, "subjectType", "USER_ID"),
                "subjectValue", stringValue(payload, "subjectValue", "user-1"),
                "status", status,
                "createdBy", actor,
                "createdAt", now());
    }

    /**
     * 执行 values 对应的 CDP 业务操作。
     */
    private static Map<String, Object> values(Object... keysAndValues) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            values.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
        }
        return values;
    }

    /**
     * 读取并校验必填的d String。
     */
    private static String requiredString(Map<String, Object> payload, String key) {
        return requiredText(stringValue(payload, key, null), key);
    }

    /**
     * 读取并校验必填的d Text。
     */
    private static String requiredText(String text, String name) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return text.trim();
    }

    /**
     * 执行 stringValue 对应的 CDP 业务操作。
     */
    private static String stringValue(Map<String, Object> payload, String key, String defaultValue) {
        if (payload == null) {
            return defaultValue;
        }
        Object value = payload.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? defaultValue : text.trim();
    }

    /**
     * 执行 longValue 对应的 CDP 业务操作。
     */
    private static Long longValue(Map<String, Object> payload, String key, Long defaultValue) {
        if (payload == null) {
            return defaultValue;
        }
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return defaultValue;
    }

    /**
     * 执行 blankToDefault 对应的 CDP 业务操作。
     */
    private static String blankToDefault(String text, String defaultValue) {
        return text == null || text.isBlank() ? defaultValue : text.trim();
    }

    /**
     * 执行 now 对应的 CDP 业务操作。
     */
    private static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
