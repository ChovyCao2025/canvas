package org.chovy.canvas.canvas.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 封装CreatorCollaborationCatalog相关的业务逻辑。
 */
public class CreatorCollaborationCatalog {

    /**
     * 保存时钟。
     */
    private final Clock clock;

    /**
     * 保存内存实现使用的creators映射数据。
     */
    private final Map<String, Map<String, Object>> creators = new LinkedHashMap<>();

    /**
     * 保存内存实现使用的campaigns映射数据。
     */
    private final Map<String, Map<String, Object>> campaigns = new LinkedHashMap<>();

    /**
     * 保存内存实现使用的collaborations映射数据。
     */
    private final Map<String, Map<String, Object>> collaborations = new LinkedHashMap<>();

    /**
     * 保存内存实现使用的deliverables映射数据。
     */
    private final Map<String, Map<String, Object>> deliverables = new LinkedHashMap<>();

    /**
     * 保存内存实现使用的mutations映射数据。
     */
    private final Map<Long, Map<String, Object>> mutations = new LinkedHashMap<>();

    /**
     * 保存next creator标识。
     */
    private long nextCreatorId = 1L;

    /**
     * 保存next campaign标识。
     */
    private long nextCampaignId = 1L;

    /**
     * 保存next collaboration标识。
     */
    private long nextCollaborationId = 1L;

    /**
     * 保存next deliverable标识。
     */
    private long nextDeliverableId = 1L;

    /**
     * 保存next mutation标识。
     */
    private long nextMutationId = 1L;

    /**
     * 创建当前对象实例。
     */
    public CreatorCollaborationCatalog(Clock clock) {
        this.clock = clock;
    }

    /**
     * 处理upsertCreator。
     */
    public synchronized Map<String, Object> upsertCreator(Long tenantId, Map<String, Object> payload, String actor) {
        Map<String, Object> safe = safePayload(payload);
        String creatorKey = requiredText(safe, "creatorKey");
        Map<String, Object> row = creators.getOrDefault(key(tenantId, creatorKey), baseRow(nextCreatorId++, tenantId,
                actor));
        row.put("creatorKey", creatorKey);
        row.put("displayName", text(safe.get("displayName"), creatorKey));
        row.put("platform", text(safe.get("platform"), "unknown"));
        touch(row, actor);
        creators.put(key(tenantId, creatorKey), row);
        return copy(row);
    }

    /**
     * 处理upsertCampaign。
     */
    public synchronized Map<String, Object> upsertCampaign(Long tenantId, Map<String, Object> payload, String actor) {
        Map<String, Object> safe = safePayload(payload);
        String campaignKey = requiredText(safe, "campaignKey");
        Map<String, Object> row = campaigns.getOrDefault(key(tenantId, campaignKey), baseRow(nextCampaignId++,
                tenantId, actor));
        row.put("campaignKey", campaignKey);
        row.put("name", text(safe.get("name"), campaignKey));
        row.put("budget", intValue(safe.get("budget"), 0));
        row.put("status", text(safe.get("status"), "ACTIVE"));
        touch(row, actor);
        campaigns.put(key(tenantId, campaignKey), row);
        return copy(row);
    }

    /**
     * 新增或更新创作者协作记录。
     */
    public synchronized Map<String, Object> upsertCollaboration(Long tenantId, Map<String, Object> payload,
                                                                String actor) {
        Map<String, Object> safe = safePayload(payload);
        String collaborationKey = requiredText(safe, "collaborationKey");
        Map<String, Object> creator = requireByKey(creators, tenantId, text(safe.get("creatorKey"), null),
                "Creator not found");
        Map<String, Object> campaign = requireByKey(campaigns, tenantId, text(safe.get("campaignKey"), null),
                "Creator campaign not found");
        Map<String, Object> row = collaborations.getOrDefault(key(tenantId, collaborationKey),
                baseRow(nextCollaborationId++, tenantId, actor));
        row.put("collaborationKey", collaborationKey);
        row.put("creatorId", creator.get("id"));
        row.put("campaignId", campaign.get("id"));
        row.put("status", text(safe.get("status"), "ACTIVE"));
        touch(row, actor);
        collaborations.put(key(tenantId, collaborationKey), row);
        return copy(row);
    }

    /**
     * 新增或更新协作交付物记录。
     */
    public synchronized Map<String, Object> upsertDeliverable(Long tenantId, Map<String, Object> payload,
                                                              String actor) {
        Map<String, Object> safe = safePayload(payload);
        String deliverableKey = requiredText(safe, "deliverableKey");
        Map<String, Object> collaboration = requireByKey(collaborations, tenantId,
                text(safe.get("collaborationKey"), null), "Creator collaboration not found");
        Map<String, Object> row = deliverables.getOrDefault(key(tenantId, deliverableKey),
                baseRow(nextDeliverableId++, tenantId, actor));
        row.put("deliverableKey", deliverableKey);
        row.put("collaborationId", collaboration.get("id"));
        row.put("contentType", text(safe.get("contentType"), "POST"));
        row.put("status", text(safe.get("status"), "PLANNED"));
        touch(row, actor);
        deliverables.put(key(tenantId, deliverableKey), row);
        return copy(row);
    }

    /**
     * 处理proposeMutation。
     */
    public synchronized Map<String, Object> proposeMutation(Long tenantId, Map<String, Object> payload, String actor) {
        Map<String, Object> safe = safePayload(payload);
        Long id = nextMutationId++;
        Map<String, Object> row = baseRow(id, tenantId, actor);
        row.put("campaignId", longValue(safe.get("campaignId"), 0L));
        row.put("collaborationId", longValue(safe.get("collaborationId"), 0L));
        row.put("mutationType", text(safe.get("mutationType"), "UPDATE"));
        row.put("status", "PROPOSED");
        row.put("approvalStatus", "PENDING");
        touch(row, actor);
        mutations.put(id, row);
        return copy(row);
    }

    /**
     * 审批创作者协作变更。
     */
    public synchronized Map<String, Object> approveMutation(Long tenantId, Long mutationId,
                                                            Map<String, Object> payload, String actor) {
        Map<String, Object> row = requireMutation(tenantId, mutationId);
        row.put("approvalStatus", "APPROVED");
        row.put("approvedBy", actor);
        row.put("approvalComment", text(safePayload(payload).get("comment"), ""));
        touch(row, actor);
        return copy(row);
    }

    /**
     * 执行已审批的创作者协作变更。
     */
    public synchronized Map<String, Object> executeMutation(Long tenantId, Long mutationId,
                                                            Map<String, Object> payload, String actor) {
        Map<String, Object> row = requireMutation(tenantId, mutationId);
        row.put("status", "EXECUTED");
        row.put("executionMode", text(safePayload(payload).get("mode"), "live"));
        row.put("executedBy", actor);
        touch(row, actor);
        return copy(row);
    }

    /**
     * 列出Mutations。
     */
    public synchronized Map<String, Object> listMutations(Long tenantId, Map<String, Object> query) {
        Map<String, Object> safe = safePayload(query);
        int limit = Math.max(1, Math.min(intValue(safe.get("limit"), 20), 100));
        List<Map<String, Object>> rows = mutations.values().stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .filter(row -> matchesLong(safe.get("campaignId"), row.get("campaignId")))
                .filter(row -> matchesLong(safe.get("collaborationId"), row.get("collaborationId")))
                .filter(row -> matchesText(safe.get("status"), row.get("status")))
                .filter(row -> matchesText(safe.get("approvalStatus"), row.get("approvalStatus")))
                .sorted(Comparator.comparing(row -> (Long) row.get("id")))
                .limit(limit)
                .map(CreatorCollaborationCatalog::copy)
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", (long) rows.size());
        result.put("records", rows);
        return result;
    }

    /**
     * 处理summary。
     */
    public synchronized Map<String, Object> summary(Long tenantId, Map<String, Object> query) {
        Map<String, Object> safe = safePayload(query);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("campaignId", longValue(safe.get("campaignId"), null));
        result.put("creatorId", longValue(safe.get("creatorId"), null));
        result.put("collaborationId", longValue(safe.get("collaborationId"), null));
        result.put("creatorCount", countTenant(creators, tenantId));
        result.put("campaignCount", countTenant(campaigns, tenantId));
        result.put("collaborationCount", countTenant(collaborations, tenantId));
        result.put("deliverableCount", countTenant(deliverables, tenantId));
        result.put("mutationCount", Math.toIntExact(mutations.values().stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .count()));
        result.put("executedMutationCount", Math.toIntExact(mutations.values().stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .filter(row -> "EXECUTED".equals(row.get("status")))
                .count()));
        result.put("evaluatedAt", text(safe.get("evaluatedAt"), now().toString()));
        return result;
    }

    /**
     * 校验并返回Mutation。
     */
    private Map<String, Object> requireMutation(Long tenantId, Long mutationId) {
        Map<String, Object> row = mutations.get(mutationId);
        if (row == null || !tenantId.equals(row.get("tenantId"))) {
            throw new IllegalArgumentException("Creator mutation not found: " + mutationId);
        }
        return row;
    }

    /**
     * 按租户和业务键查询必需的数据行。
     */
    private static Map<String, Object> requireByKey(Map<String, Map<String, Object>> rows, Long tenantId,
                                                    String value, String message) {
        Map<String, Object> row = value == null ? null : rows.get(key(tenantId, value));
        if (row == null) {
            throw new IllegalArgumentException(message + ": " + value);
        }
        return row;
    }

    /**
     * 处理countTenant。
     */
    private static int countTenant(Map<String, Map<String, Object>> rows, Long tenantId) {
        return Math.toIntExact(rows.values().stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .count());
    }

    /**
     * 处理baseRow。
     */
    private Map<String, Object> baseRow(Long id, Long tenantId, String actor) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("tenantId", tenantId);
        row.put("createdBy", actor);
        row.put("createdAt", now());
        return row;
    }

    /**
     * 转换为uch。
     */
    private void touch(Map<String, Object> row, String actor) {
        row.put("updatedBy", actor);
        row.put("updatedAt", now());
    }

    /**
     * 处理now。
     */
    private Instant now() {
        return Instant.now(clock);
    }

    /**
     * 处理键。
     */
    private static String key(Long tenantId, String value) {
        return tenantId + ":" + value;
    }

    /**
     * 校验并返回dText。
     */
    private static String requiredText(Map<String, Object> payload, String key) {
        String value = text(payload.get(key), null);
        if (value == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    /**
     * 处理safePayload。
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    }

    /**
     * 处理copy。
     */
    private static Map<String, Object> copy(Map<String, Object> row) {
        return new LinkedHashMap<>(row);
    }

    /**
     * 处理matchesText。
     */
    private static boolean matchesText(Object expected, Object actual) {
        return expected == null || expected.toString().isBlank() || expected.equals(actual);
    }

    /**
     * 处理matchesLong。
     */
    private static boolean matchesLong(Object expected, Object actual) {
        return expected == null || longValue(expected, Long.MIN_VALUE).equals(actual);
    }

    /**
     * 处理text。
     */
    private static String text(Object value, String fallback) {
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString().trim();
    }

    /**
     * 处理intValue。
     */
    private static Integer intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value.toString());
    }

    /**
     * 处理longValue。
     */
    private static Long longValue(Object value, Long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return Long.parseLong(value.toString());
    }
}
