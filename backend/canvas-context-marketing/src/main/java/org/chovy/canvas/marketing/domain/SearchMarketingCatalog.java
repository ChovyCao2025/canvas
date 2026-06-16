package org.chovy.canvas.marketing.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 维护SearchMarketing相关的内存业务目录。
 */
public class SearchMarketingCatalog {

    /**
     * 用于生成确定性业务时间的时钟。
     */
    private final Clock clock;
    private final Map<Long, TenantState> tenants = new LinkedHashMap<>();

    /**
     * 创建SearchMarketingCatalog实例。
     */
    public SearchMarketingCatalog(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 查询sources列表。
     */
    public List<Map<String, Object>> listSources(Long tenantId, String provider, String channel,
                                                 Boolean enabled, int limit) {
        return list(state(tenantId).sources, limit, row -> matches(row.get("provider"), provider)
                && matches(row.get("channel"), channel)
                && (enabled == null || Objects.equals(row.get("enabled"), enabled)));
    }

    /**
     * 执行upsertSource业务操作。
     */
    public Map<String, Object> upsertSource(Long tenantId, Map<String, Object> payload, String actor) {
        TenantState state = state(tenantId);
        Map<String, Object> safePayload = safeMap(payload);
        String provider = key(safePayload, "provider", "provider");
        Map<String, Object> row = append(state.sources, state.nextSourceId++, tenantId, "upsertSource", safePayload,
                actor, "ACTIVE");
        row.put("provider", normalized(provider, "GOOGLE"));
        row.put("sourceKey", key(safePayload, "sourceKey", "source-" + row.get("id")));
        row.put("channel", normalized(safePayload.get("channel"), "SEO"));
        row.put("enabled", booleanValue(safePayload.get("enabled"), true));
        return copy(row);
    }

    /**
     * 查询keywords列表。
     */
    public List<Map<String, Object>> listKeywords(Long tenantId, String channel, String status, int limit) {
        return list(state(tenantId).keywords, limit, row -> matches(row.get("channel"), channel)
                && matches(row.get("status"), status));
    }

    /**
     * 执行upsertKeyword业务操作。
     */
    public Map<String, Object> upsertKeyword(Long tenantId, Map<String, Object> payload, String actor) {
        TenantState state = state(tenantId);
        Map<String, Object> row = append(state.keywords, state.nextKeywordId++, tenantId, "upsertKeyword",
                safeMap(payload), actor, "ACTIVE");
        row.put("keywordText", key(row, "keywordText", "keyword-" + row.get("id")));
        row.put("channel", normalized(row.get("channel"), "SEO"));
        return copy(row);
    }

    /**
     * 查询snapshots列表。
     */
    public List<Map<String, Object>> listSnapshots(Long tenantId, String channel, Long sourceId, Long keywordId,
                                                   LocalDate startDate, LocalDate endDate, int limit) {
        return list(state(tenantId).snapshots, limit, row -> matches(row.get("channel"), channel)
                && idMatches(row.get("sourceId"), sourceId)
                && idMatches(row.get("keywordId"), keywordId)
                && dateInRange((LocalDate) row.get("snapshotDate"), startDate, endDate));
    }

    /**
     * 执行upsertSnapshot业务操作。
     */
    public Map<String, Object> upsertSnapshot(Long tenantId, Map<String, Object> payload, String actor) {
        TenantState state = state(tenantId);
        Map<String, Object> row = append(state.snapshots, state.nextSnapshotId++, tenantId, "upsertSnapshot",
                safeMap(payload), actor, "RECORDED");
        row.put("sourceId", toLong(row.get("sourceId"), null));
        row.put("keywordId", toLong(row.get("keywordId"), null));
        row.put("channel", normalized(row.get("channel"), "SEO"));
        row.put("snapshotDate", toDate(row.get("snapshotDate"), LocalDate.now(clock)));
        row.put("impressionCount", toLong(row.get("impressionCount"), 0L));
        row.put("clickCount", toLong(row.get("clickCount"), 0L));
        return copy(row);
    }

    /**
     * 查询opportunities列表。
     */
    public List<Map<String, Object>> listOpportunities(Long tenantId, String channel, Long sourceId,
                                                       String status, String severity, int limit) {
        return list(state(tenantId).opportunities, limit, row -> matches(row.get("channel"), channel)
                && idMatches(row.get("sourceId"), sourceId)
                && matches(row.get("status"), status)
                && matches(row.get("severity"), severity));
    }

    /**
     * 执行evaluateOpportunities业务操作。
     */
    public Map<String, Object> evaluateOpportunities(Long tenantId, Map<String, Object> payload, String actor) {
        TenantState state = state(tenantId);
        Map<String, Object> row = append(state.opportunities, state.nextOpportunityId++, tenantId,
                "evaluateOpportunities", safeMap(payload), actor, "OPEN");
        row.put("channel", normalized(row.get("channel"), "SEO"));
        row.put("sourceId", toLong(row.get("sourceId"), null));
        row.put("severity", normalized(row.get("severity"), "MEDIUM"));
        return copy(row);
    }

    /**
     * 更新opportunityStatus业务对象。
     */
    public Map<String, Object> updateOpportunityStatus(Long tenantId, Long opportunityId,
                                                       Map<String, Object> payload, String actor) {
        Map<String, Object> safePayload = safeMap(payload);
        String status = requiredText(safePayload.get("status"), "status");
        Map<String, Object> row = findOrCreate(state(tenantId).opportunities, opportunityId, tenantId,
                "updateOpportunityStatus", safePayload, actor, "OPEN");
        row.putAll(safePayload);
        row.put("status", normalized(status, "OPEN"));
        row.put("updatedBy", actor);
        return copy(row);
    }

    /**
     * 创建mutation业务对象。
     */
    public Map<String, Object> createMutation(Long tenantId, Long opportunityId, Map<String, Object> payload,
                                              String actor) {
        TenantState state = state(tenantId);
        Map<String, Object> row = append(state.mutations, state.nextMutationId++, tenantId,
                opportunityId == null ? "upsertMutation" : "createOpportunityMutation", safeMap(payload), actor,
                "PENDING");
        row.put("opportunityId", opportunityId);
        row.put("sourceId", toLong(row.get("sourceId"), null));
        row.put("mutationKey", key(row, "mutationKey", "mutation-" + row.get("id")));
        row.put("approvalStatus", normalized(row.get("approvalStatus"), "PENDING"));
        return copy(row);
    }

    /**
     * 查询mutations列表。
     */
    public List<Map<String, Object>> listMutations(Long tenantId, Long sourceId, String status,
                                                   String approvalStatus, int limit) {
        return list(state(tenantId).mutations, limit, row -> idMatches(row.get("sourceId"), sourceId)
                && matches(row.get("status"), status)
                && matches(row.get("approvalStatus"), approvalStatus));
    }

    /**
     * 执行approveMutation业务操作。
     */
    public Map<String, Object> approveMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                               String actor) {
        Map<String, Object> row = findOrCreate(state(tenantId).mutations, mutationId, tenantId, "approveMutation",
                safeMap(payload), actor, "PENDING");
        row.putAll(safeMap(payload));
        row.put("approvalStatus", normalized(row.get("decision"), "APPROVED"));
        row.put("updatedBy", actor);
        return copy(row);
    }

    /**
     * 执行executeMutation业务操作。
     */
    public Map<String, Object> executeMutation(Long tenantId, Long mutationId, Map<String, Object> payload,
                                               String actor) {
        Map<String, Object> row = findOrCreate(state(tenantId).mutations, mutationId, tenantId, "executeMutation",
                safeMap(payload), actor, "PENDING");
        row.putAll(safeMap(payload));
        row.put("status", Boolean.TRUE.equals(row.get("dryRun")) ? "DRY_RUN" : "EXECUTED");
        row.put("updatedBy", actor);
        return copy(row);
    }

    /**
     * 查询urlInspections列表。
     */
    public List<Map<String, Object>> listUrlInspections(Long tenantId, Long sourceId, String indexedState,
                                                        LocalDate startDate, LocalDate endDate, int limit) {
        TenantState state = state(tenantId);
        if (state.urlInspections.isEmpty()) {
            Map<String, Object> row = append(state.urlInspections, state.nextUrlInspectionId++, tenantId,
                    "listUrlInspections", Map.of(), "system", "INDEXED");
            row.put("sourceId", sourceId);
            row.put("indexedState", indexedState == null ? "INDEXED" : indexedState);
            row.put("inspectionDate", startDate == null ? LocalDate.now(clock) : startDate);
        }
        return list(state.urlInspections, limit, row -> idMatches(row.get("sourceId"), sourceId)
                && matches(row.get("indexedState"), indexedState)
                && dateInRange((LocalDate) row.get("inspectionDate"), startDate, endDate));
    }

    /**
     * 查询syncRuns列表。
     */
    public List<Map<String, Object>> listSyncRuns(Long tenantId, Long sourceId, String runType, String status,
                                                  int limit) {
        return list(state(tenantId).syncRuns, limit, row -> idMatches(row.get("sourceId"), sourceId)
                && matches(row.get("runType"), runType)
                && matches(row.get("status"), status));
    }

    /**
     * 执行syncSource业务操作。
     */
    public Map<String, Object> syncSource(Long tenantId, Long sourceId, Map<String, Object> payload, String actor) {
        TenantState state = state(tenantId);
        Map<String, Object> safePayload = safeMap(payload);
        Map<String, Object> row = append(state.syncRuns, state.nextSyncRunId++, tenantId, "syncSource", safePayload,
                actor, "SUCCEEDED");
        row.put("sourceId", sourceId);
        row.put("runType", normalized(row.get("runType"), "PERFORMANCE"));
        return copy(row);
    }

    /**
     * 执行syncDue业务操作。
     */
    public Map<String, Object> syncDue(Long tenantId, Map<String, Object> payload, String actor) {
        int limit = limitFrom(payload);
        Map<String, Object> row = base(tenantId, "syncDue", safeMap(payload), actor, "SCHEDULED");
        row.put("limit", limit);
        row.put("scheduledCount", Math.min(limit, Math.max(1, state(tenantId).sources.size())));
        return row;
    }

    /**
     * 查询providerChanges列表。
     */
    public List<Map<String, Object>> listProviderChanges(Long tenantId, Long sourceId, Long mutationId,
                                                         String provider, String reconciliationStatus, int limit) {
        return list(state(tenantId).providerChanges, limit, row -> idMatches(row.get("sourceId"), sourceId)
                && idMatches(row.get("mutationId"), mutationId)
                && matches(row.get("provider"), provider)
                && matches(row.get("reconciliationStatus"), reconciliationStatus));
    }

    /**
     * 执行reconcileMutation业务操作。
     */
    public Map<String, Object> reconcileMutation(Long tenantId, Long mutationId, String actor) {
        TenantState state = state(tenantId);
        Map<String, Object> mutation = findOrCreate(state.mutations, mutationId, tenantId, "reconcileMutation",
                Map.of("mutationId", mutationId), actor, "EXECUTED");
        mutation.put("reconciliationStatus", "RECONCILED");
        Map<String, Object> change = append(state.providerChanges, state.nextProviderChangeId++, tenantId,
                "reconcileMutation", Map.of("mutationId", mutationId), actor, "RECONCILED");
        change.put("mutationId", mutationId);
        change.put("sourceId", mutation.get("sourceId"));
        change.put("provider", key(mutation, "provider", "google"));
        change.put("reconciliationStatus", "RECONCILED");
        appendImpactWindow(state, tenantId, mutationId, toLong(mutation.get("sourceId"), null), actor);
        return copy(mutation);
    }

    /**
     * 查询impactWindows列表。
     */
    public List<Map<String, Object>> listImpactWindows(Long tenantId, Long opportunityId, Long mutationId,
                                                       Long sourceId, String status, String decision, int limit) {
        return list(state(tenantId).impactWindows, limit, row -> idMatches(row.get("opportunityId"), opportunityId)
                && idMatches(row.get("mutationId"), mutationId)
                && idMatches(row.get("sourceId"), sourceId)
                && matches(row.get("status"), status)
                && matches(row.get("decision"), decision));
    }

    /**
     * 执行evaluateDueImpactWindows业务操作。
     */
    public Map<String, Object> evaluateDueImpactWindows(Long tenantId, Map<String, Object> payload, String actor) {
        int limit = limitFrom(payload);
        Map<String, Object> row = base(tenantId, "evaluateDueImpactWindows", safeMap(payload), actor, "EVALUATED");
        row.put("limit", limit);
        row.put("evaluatedCount", Math.min(limit, Math.max(1, state(tenantId).impactWindows.size())));
        return row;
    }

    /**
     * 执行readiness业务操作。
     */
    public Map<String, Object> readiness(Long tenantId) {
        TenantState state = state(tenantId);
        boolean ready = !state.sources.isEmpty();
        Map<String, Object> row = base(tenantId, "readiness", Map.of(), "system", ready ? "READY" : "BLOCKED");
        row.put("productionReady", ready);
        row.put("sourceCount", state.sources.size());
        row.put("keywordCount", state.keywords.size());
        return row;
    }

    /**
     * 执行summary业务操作。
     */
    public Map<String, Object> summary(Long tenantId, String channel, Long sourceId, Long keywordId,
                                       LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> snapshots = listSnapshots(tenantId, channel, sourceId, keywordId, startDate, endDate,
                100);
        Map<String, Object> row = base(tenantId, "summary", Map.of(), "system", "READY");
        row.put("channel", channel);
        row.put("sourceId", sourceId);
        row.put("keywordId", keywordId);
        row.put("snapshotCount", snapshots.size());
        row.put("sourceCount", state(tenantId).sources.size());
        row.put("impressionCount", sum(snapshots, "impressionCount"));
        row.put("clickCount", sum(snapshots, "clickCount"));
        return row;
    }

    /**
     * 执行appendImpactWindow业务操作。
     */
    private void appendImpactWindow(TenantState state, Long tenantId, Long mutationId, Long sourceId, String actor) {
        Map<String, Object> row = append(state.impactWindows, state.nextImpactWindowId++, tenantId,
                "impactWindow", Map.of("mutationId", mutationId), actor, "PENDING");
        row.put("mutationId", mutationId);
        row.put("sourceId", sourceId);
        row.put("decision", "KEEP");
    }

    /**
     * 执行append业务操作。
     */
    private Map<String, Object> append(List<Map<String, Object>> rows, long id, Long tenantId, String operation,
                                       Map<String, Object> payload, String actor, String status) {
        Map<String, Object> row = base(tenantId, operation, payload, actor, status);
        row.put("id", id);
        row.putAll(payload);
        row.put("status", normalized(row.get("status"), status));
        row.put("createdBy", actor);
        row.put("updatedBy", actor);
        rows.add(row);
        return row;
    }

    /**
     * 查找orCreate业务对象。
     */
    private Map<String, Object> findOrCreate(List<Map<String, Object>> rows, Long id, Long tenantId, String operation,
                                             Map<String, Object> payload, String actor, String status) {
        Long requiredId = id == null ? 1L : id;
        return rows.stream()
                .filter(row -> Objects.equals(row.get("id"), requiredId))
                .findFirst()
                .orElseGet(() -> {
                    Map<String, Object> created = append(rows, requiredId, tenantId, operation, payload, actor, status);
                    created.put("id", requiredId);
                    return created;
                });
    }

    /**
     * 执行base业务操作。
     */
    private Map<String, Object> base(Long tenantId, String operation, Map<String, Object> payload, String actor,
                                     String status) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tenantId", tenantId);
        row.put("operation", operation);
        row.put("payload", new LinkedHashMap<>(payload));
        row.put("status", status);
        row.put("occurredAt", clock.instant().toString());
        row.put("updatedBy", actor);
        return row;
    }

    /**
     * 执行state业务操作。
     */
    private TenantState state(Long tenantId) {
        return tenants.computeIfAbsent(tenantId, ignored -> new TenantState());
    }

    /**
     * 查询列表。
     */
    private static List<Map<String, Object>> list(List<Map<String, Object>> rows, int limit,
                                                  java.util.function.Predicate<Map<String, Object>> filter) {
        return rows.stream()
                .filter(filter)
                .sorted(Comparator.comparing(row -> Long.parseLong(String.valueOf(row.get("id")))))
                .limit(limit)
                .map(SearchMarketingCatalog::copy)
                .toList();
    }

    /**
     * 执行matches业务操作。
     */
    private static boolean matches(Object rowValue, Object expected) {
        return expected == null || String.valueOf(expected).isBlank()
                || (rowValue != null && String.valueOf(rowValue).equalsIgnoreCase(String.valueOf(expected)));
    }

    /**
     * 执行idMatches业务操作。
     */
    private static boolean idMatches(Object rowValue, Long expected) {
        return expected == null || Objects.equals(toLong(rowValue, null), expected);
    }

    /**
     * 执行dateInRange业务操作。
     */
    private static boolean dateInRange(LocalDate value, LocalDate startDate, LocalDate endDate) {
        return value != null
                && (startDate == null || !value.isBefore(startDate))
                && (endDate == null || !value.isAfter(endDate));
    }

    /**
     * 规范化d输入值。
     */
    private static String normalized(Object value, Object fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.isBlank()) {
            text = fallback == null ? "" : String.valueOf(fallback);
        }
        return text.isBlank() ? null : text.toUpperCase();
    }

    /**
     * 执行key业务操作。
     */
    private static String key(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    /**
     * 校验并返回dText必填值。
     */
    private static String requiredText(Object value, String field) {
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return text;
    }

    /**
     * 执行booleanValue业务操作。
     */
    private static boolean booleanValue(Object value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 执行limitFrom业务操作。
     */
    private static int limitFrom(Map<String, Object> payload) {
        return Math.max(1, Math.min(toLong(safeMap(payload).get("limit"), 50L).intValue(), 100));
    }

    /**
     * 转换为long对象。
     */
    private static Long toLong(Object value, Long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    /**
     * 转换为date对象。
     */
    private static LocalDate toDate(Object value, LocalDate fallback) {
        return value instanceof LocalDate date ? date : value == null ? fallback : LocalDate.parse(String.valueOf(value));
    }

    /**
     * 执行sum业务操作。
     */
    private static long sum(List<Map<String, Object>> rows, String field) {
        return rows.stream()
                .map(row -> toLong(row.get(field), 0L))
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
    }

    /**
     * 执行safeMap业务操作。
     */
    private static Map<String, Object> safeMap(Map<String, Object> payload) {
        return payload == null ? Map.of() : new LinkedHashMap<>(payload);
    }

    /**
     * 执行copy业务操作。
     */
    private static Map<String, Object> copy(Map<String, Object> row) {
        return new LinkedHashMap<>(row);
    }

    /**
     * 提供TenantState的业务能力。
     */
    private static final class TenantState {
        private final List<Map<String, Object>> sources = new ArrayList<>();
        private final List<Map<String, Object>> keywords = new ArrayList<>();
        private final List<Map<String, Object>> snapshots = new ArrayList<>();
        private final List<Map<String, Object>> opportunities = new ArrayList<>();
        private final List<Map<String, Object>> mutations = new ArrayList<>();
        private final List<Map<String, Object>> urlInspections = new ArrayList<>();
        private final List<Map<String, Object>> syncRuns = new ArrayList<>();
        private final List<Map<String, Object>> providerChanges = new ArrayList<>();
        private final List<Map<String, Object>> impactWindows = new ArrayList<>();

        /**
         * 保存nextSourceId字段值。
         */
        private long nextSourceId = 1L;

        /**
         * 保存nextKeywordId字段值。
         */
        private long nextKeywordId = 1L;

        /**
         * 保存nextSnapshotId字段值。
         */
        private long nextSnapshotId = 1L;

        /**
         * 保存nextOpportunityId字段值。
         */
        private long nextOpportunityId = 1L;

        /**
         * 保存nextMutationId字段值。
         */
        private long nextMutationId = 1L;

        /**
         * 保存nextUrlInspectionId字段值。
         */
        private long nextUrlInspectionId = 1L;

        /**
         * 保存nextSyncRunId字段值。
         */
        private long nextSyncRunId = 1L;

        /**
         * 保存nextProviderChangeId字段值。
         */
        private long nextProviderChangeId = 1L;

        /**
         * 保存nextImpactWindowId字段值。
         */
        private long nextImpactWindowId = 1L;
    }
}
