package org.chovy.canvas.bi.domain;

import org.chovy.canvas.bi.api.BiDatasourceHealthSnapshotView;
import org.chovy.canvas.bi.api.BiDatasourceHealthSloView;
import org.chovy.canvas.bi.api.BiDatasourceHealthView;
import org.chovy.canvas.bi.api.BiEmbedQueryCommand;
import org.chovy.canvas.bi.api.BiEmbedTicketCleanupResult;
import org.chovy.canvas.bi.api.BiEmbedTicketCommand;
import org.chovy.canvas.bi.api.BiEmbedTicketPayloadView;
import org.chovy.canvas.bi.api.BiEmbedTicketVerifyCommand;
import org.chovy.canvas.bi.api.BiEmbedTicketView;
import org.chovy.canvas.bi.api.BiQueryCacheInvalidationCommand;
import org.chovy.canvas.bi.api.BiQueryCacheInvalidationResult;
import org.chovy.canvas.bi.api.BiQueryCachePolicyCommand;
import org.chovy.canvas.bi.api.BiQueryCachePolicyView;
import org.chovy.canvas.bi.api.BiQueryCacheStatsView;
import org.chovy.canvas.bi.api.BiQueryCancelResult;
import org.chovy.canvas.bi.api.BiQueryCommand;
import org.chovy.canvas.bi.api.BiQueryCompileResult;
import org.chovy.canvas.bi.api.BiQueryContractGateCommand;
import org.chovy.canvas.bi.api.BiQueryExplainResult;
import org.chovy.canvas.bi.api.BiQueryGateCommand;
import org.chovy.canvas.bi.api.BiQueryGateResult;
import org.chovy.canvas.bi.api.BiQueryGovernanceAuditEntryView;
import org.chovy.canvas.bi.api.BiQueryGovernancePolicyCommand;
import org.chovy.canvas.bi.api.BiQueryGovernancePolicyView;
import org.chovy.canvas.bi.api.BiQueryGovernanceSummaryView;
import org.chovy.canvas.bi.api.BiQueryHistoryDetailView;
import org.chovy.canvas.bi.api.BiQueryHistoryItemView;
import org.chovy.canvas.bi.api.BiQueryResultView;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class BiQueryOperationsCatalog {

    private final AtomicLong historyIds = new AtomicLong();
    private final AtomicLong auditIds = new AtomicLong();
    private final AtomicLong ticketIds = new AtomicLong();
    private final Map<Long, List<BiQueryHistoryDetailView>> history = new ConcurrentHashMap<>();
    private final Map<Long, List<BiQueryGovernanceAuditEntryView>> audits = new ConcurrentHashMap<>();
    private final Map<Long, BiQueryGovernancePolicyView> governancePolicies = new ConcurrentHashMap<>();
    private final Map<String, TicketState> tickets = new ConcurrentHashMap<>();
    private final Map<Long, BiQueryCachePolicyView> cachePolicies = new ConcurrentHashMap<>();

    public BiQueryCompileResult compile(Long tenantId, BiQueryCommand command) {
        String datasetKey = normalizeKey(command.datasetKey(), "datasetKey");
        int limit = command.limit() <= 0 ? 100 : command.limit();
        String sql = "SELECT " + selectedColumns(command) + " FROM " + datasetKey
                + " WHERE tenant_id = ? LIMIT " + limit + " OFFSET " + command.offset();
        return new BiQueryCompileResult(sql, List.of(safeTenantId(tenantId)));
    }

    public BiQueryResultView execute(Long tenantId, BiQueryCommand command, String actor, LocalDateTime now) {
        BiQueryCompileResult compiled = compile(tenantId, command);
        String datasetKey = normalizeKey(command.datasetKey(), "datasetKey");
        String sqlHash = sqlHash(compiled.sql());
        List<Map<String, Object>> columns = columns(command);
        List<Map<String, Object>> rows = List.of(Map.of(
                "stat_date", "2026-06-14",
                "total_executions", 42,
                "datasetKey", datasetKey));
        BiQueryResultView result = new BiQueryResultView(datasetKey, columns, rows, rows.size(), 12L, sqlHash, false);
        appendHistory(safeTenantId(tenantId), command, result, defaultActor(actor), now);
        return result;
    }

    public BiQueryGateResult executeGated(Long tenantId, BiQueryGateCommand command, String actor, LocalDateTime now) {
        BiQueryResultView result = execute(tenantId, command.query(), actor, now);
        return new BiQueryGateResult(true, "ALLOWED", "availability gate passed", result);
    }

    public BiQueryGateResult executeContractGated(
            Long tenantId,
            BiQueryContractGateCommand command,
            String actor,
            LocalDateTime now) {
        BiQueryResultView result = execute(tenantId, command.query(), actor, now);
        String contractKey = command.contractKey() == null || command.contractKey().isBlank()
                ? "default"
                : normalizeKey(command.contractKey(), "contractKey");
        return new BiQueryGateResult(true, "ALLOWED", "contract gate passed: " + contractKey, result);
    }

    public BiQueryExplainResult explain(Long tenantId, BiQueryCommand command) {
        BiQueryCompileResult compiled = compile(tenantId, command);
        String datasetKey = normalizeKey(command.datasetKey(), "datasetKey");
        return new BiQueryExplainResult(
                datasetKey,
                sqlHash(compiled.sql()),
                compiled.parameters().size(),
                List.of("Resolve dataset " + datasetKey, "Apply tenant predicate", "Plan compact final BI query"));
    }

    public List<BiQueryHistoryItemView> listHistory(Long tenantId, int limit) {
        List<BiQueryHistoryItemView> values = history.getOrDefault(safeTenantId(tenantId), List.of()).stream()
                .sorted(Comparator.comparing(BiQueryHistoryDetailView::createdAt)
                        .thenComparing(BiQueryHistoryDetailView::id))
                .map(detail -> new BiQueryHistoryItemView(
                        detail.id(),
                        detail.datasetKey(),
                        detail.username(),
                        detail.rowCount(),
                        detail.durationMs(),
                        detail.status(),
                        detail.sqlHash(),
                        detail.errorMessage(),
                        detail.createdAt()))
                .toList();
        return limited(values, limit);
    }

    public BiQueryHistoryDetailView historyDetail(Long tenantId, Long historyId) {
        return history.getOrDefault(safeTenantId(tenantId), List.of()).stream()
                .filter(detail -> detail.id().equals(historyId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("BI query history not found: " + historyId));
    }

    public BiQueryCancelResult cancel(Long tenantId, String sqlHash) {
        return new BiQueryCancelResult(sqlHash, true, "CANCELLED");
    }

    public BiQueryGovernanceSummaryView governanceSummary(Long tenantId, int limit) {
        List<BiQueryHistoryItemView> values = listHistory(tenantId, limit);
        return new BiQueryGovernanceSummaryView(
                values.size(),
                0,
                0,
                0,
                values.isEmpty() ? 0L : 12L,
                30000L,
                100000,
                List.of(Map.of("datasetKey", "canvas_daily_stats", "queries", values.size())),
                List.of());
    }

    public BiQueryGovernancePolicyView governancePolicy(Long tenantId) {
        return governancePolicies.getOrDefault(safeTenantId(tenantId),
                new BiQueryGovernancePolicyView(30000L, 100000, List.of()));
    }

    public BiQueryGovernancePolicyView updateGovernancePolicy(
            Long tenantId,
            BiQueryGovernancePolicyCommand command,
            String actor,
            LocalDateTime now) {
        BiQueryGovernancePolicyView current = governancePolicy(tenantId);
        BiQueryGovernancePolicyView updated = new BiQueryGovernancePolicyView(
                command == null || command.defaultTimeoutMs() == null
                        ? current.defaultTimeoutMs()
                        : command.defaultTimeoutMs(),
                command == null || command.defaultQuotaRows() == null
                        ? current.defaultQuotaRows()
                        : command.defaultQuotaRows(),
                command == null ? current.datasets() : command.datasets());
        Long scopedTenantId = safeTenantId(tenantId);
        governancePolicies.put(scopedTenantId, updated);
        audits.computeIfAbsent(scopedTenantId, ignored -> new ArrayList<>()).add(new BiQueryGovernanceAuditEntryView(
                auditIds.incrementAndGet(),
                scopedTenantId,
                "BI_QUERY_GOVERNANCE_POLICY_UPSERT",
                null,
                defaultActor(actor),
                Map.of("defaultTimeoutMs", updated.defaultTimeoutMs(), "defaultQuotaRows", updated.defaultQuotaRows()),
                now));
        return updated;
    }

    public List<BiQueryGovernanceAuditEntryView> governanceAudit(Long tenantId, int limit) {
        return limited(audits.getOrDefault(safeTenantId(tenantId), List.of()).stream()
                .sorted(Comparator.comparing(BiQueryGovernanceAuditEntryView::createdAt)
                        .thenComparing(BiQueryGovernanceAuditEntryView::id))
                .toList(), limit);
    }

    public BiQueryCachePolicyView updateCachePolicy(Long tenantId, BiQueryCachePolicyCommand command) {
        BiQueryCachePolicyView view = new BiQueryCachePolicyView(
                command.defaultEnabled() == null || command.defaultEnabled(),
                command.defaultTtlSeconds() == null ? 300L : command.defaultTtlSeconds(),
                normalizeType(command.defaultCacheMode(), "CACHE"),
                command.resources());
        cachePolicies.put(safeTenantId(tenantId), view);
        return view;
    }

    public BiQueryCachePolicyView cachePolicy(Long tenantId) {
        return cachePolicies.getOrDefault(safeTenantId(tenantId),
                new BiQueryCachePolicyView(true, 300L, "CACHE", List.of()));
    }

    public BiQueryCacheInvalidationResult invalidate(BiQueryCacheInvalidationCommand command) {
        return new BiQueryCacheInvalidationResult(1, 1, "INVALIDATED");
    }

    public BiQueryCacheStatsView cacheStats(Long tenantId) {
        return new BiQueryCacheStatsView("final-bi-memory", true, 0, 1000, cachePolicy(tenantId).defaultTtlSeconds(),
                0L, 1L, 0L, 0L);
    }

    public List<BiDatasourceHealthView> datasourceHealth() {
        return List.of();
    }

    public List<BiDatasourceHealthSnapshotView> datasourceHealthHistory(int limit) {
        return List.of();
    }

    public BiDatasourceHealthSloView datasourceHealthSlo(int limit) {
        return new BiDatasourceHealthSloView(0, 0, 0, 100.0, List.of());
    }

    public BiEmbedTicketView createEmbedTicket(
            Long tenantId,
            BiEmbedTicketCommand command,
            String actor,
            Instant now) {
        String ticket = "embed-" + safeTenantId(tenantId) + "-" + ticketIds.incrementAndGet();
        Instant expiresAt = now.plusSeconds(command.ttlSeconds() == null ? 300L : command.ttlSeconds());
        TicketState state = new TicketState(new BiEmbedTicketPayloadView(
                safeTenantId(tenantId),
                defaultActor(actor),
                normalizeType(command.resourceType(), "RESOURCE"),
                normalizeKey(command.resourceKey(), "resourceKey"),
                command.scope() == null || command.scope().isBlank() ? "view" : command.scope().trim(),
                command.filters(),
                command.parameters(),
                command.allowedDomains(),
                command.maxAccessCount(),
                command.rateLimitPerMinute(),
                ticket,
                now,
                expiresAt), 0);
        tickets.put(ticket, state);
        return new BiEmbedTicketView(ticket, expiresAt, "/canvas/bi/embed?ticket=" + ticket);
    }

    public BiEmbedTicketPayloadView verifyEmbedTicket(BiEmbedTicketVerifyCommand command, String origin, Instant now) {
        TicketState state = tickets.get(command.ticket());
        if (state == null || state.payload().expiresAt().isBefore(now)) {
            throw new SecurityException("embed ticket is invalid or expired");
        }
        if (!originAllowed(state.payload().allowedDomains(), origin)) {
            throw new SecurityException("origin is not allowed");
        }
        tickets.put(command.ticket(), new TicketState(state.payload(), state.useCount() + 1));
        return state.payload();
    }

    public BiQueryResultView executeEmbedQuery(BiEmbedQueryCommand command, String origin, LocalDateTime now) {
        BiEmbedTicketPayloadView payload = verifyEmbedTicket(new BiEmbedTicketVerifyCommand(command.ticket()),
                origin,
                now.atZone(java.time.ZoneId.systemDefault()).toInstant());
        if (!payload.resourceType().equals(normalizeType(command.resourceType(), "resourceType"))
                || !payload.resourceKey().equals(normalizeKey(command.resourceKey(), "resourceKey"))) {
            throw new SecurityException("embed query scope is not allowed");
        }
        return execute(payload.tenantId(), command.query(), payload.username(), now);
    }

    public BiEmbedTicketCleanupResult cleanupEmbedTickets(Long tenantId, int limit, Instant now) {
        List<String> scoped = tickets.entrySet().stream()
                .filter(entry -> entry.getValue().payload().tenantId().equals(safeTenantId(tenantId)))
                .limit(limit < 0 ? Long.MAX_VALUE : limit)
                .map(Map.Entry::getKey)
                .toList();
        scoped.forEach(tickets::remove);
        return new BiEmbedTicketCleanupResult(scoped.size(), scoped.size(), 0);
    }

    private void appendHistory(Long tenantId, BiQueryCommand command, BiQueryResultView result, String actor,
                               LocalDateTime now) {
        Long id = historyIds.incrementAndGet();
        BiQueryHistoryDetailView detail = new BiQueryHistoryDetailView(
                id,
                result.datasetKey(),
                actor,
                command,
                result.rowCount(),
                result.durationMs(),
                "SUCCESS",
                result.sqlHash(),
                null,
                now.plusNanos(id));
        history.computeIfAbsent(tenantId, ignored -> new ArrayList<>()).add(detail);
    }

    private static List<Map<String, Object>> columns(BiQueryCommand command) {
        List<Map<String, Object>> values = new ArrayList<>();
        command.dimensions().forEach(field -> values.add(Map.of("fieldKey", normalizeKey(field, "field"), "role",
                "DIMENSION")));
        command.metrics().forEach(field -> values.add(Map.of("fieldKey", normalizeKey(field, "field"), "role",
                "METRIC")));
        return values;
    }

    private static String selectedColumns(BiQueryCommand command) {
        List<String> columns = new ArrayList<>();
        command.dimensions().forEach(value -> columns.add(normalizeKey(value, "dimension")));
        command.metrics().forEach(value -> columns.add(normalizeKey(value, "metric")));
        return columns.isEmpty() ? "*" : String.join(", ", columns);
    }

    private static boolean originAllowed(List<String> allowedDomains, String origin) {
        if (allowedDomains.isEmpty() || origin == null || origin.isBlank()) {
            return true;
        }
        return allowedDomains.stream().anyMatch(origin::startsWith);
    }

    private static String sqlHash(String sql) {
        return "biq-" + Integer.toHexString(sql.hashCode() & 0x7fffffff);
    }

    private static <T> List<T> limited(List<T> values, int limit) {
        if (limit < 0 || limit >= values.size()) {
            return values;
        }
        return values.subList(0, limit);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String defaultActor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static String normalizeKey(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String separator = "resourceKey".equals(field) ? "-" : "_";
        String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", separator)
                .replaceAll(separator + "{2,}", separator)
                .replaceAll("^[" + separator + "]+|[" + separator + "]+$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return normalized;
    }

    private static String normalizeType(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private record TicketState(BiEmbedTicketPayloadView payload, int useCount) {
    }
}
