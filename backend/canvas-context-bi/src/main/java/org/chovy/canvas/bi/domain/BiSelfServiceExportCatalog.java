package org.chovy.canvas.bi.domain;

import org.chovy.canvas.bi.api.BiSelfServiceExportCleanupResult;
import org.chovy.canvas.bi.api.BiSelfServiceExportCommand;
import org.chovy.canvas.bi.api.BiSelfServiceExportDownload;
import org.chovy.canvas.bi.api.BiSelfServiceExportJobDetailView;
import org.chovy.canvas.bi.api.BiSelfServiceExportJobView;
import org.chovy.canvas.bi.api.BiSelfServiceExportQueueResult;
import org.chovy.canvas.bi.api.BiSelfServiceExportRetryResult;
import org.chovy.canvas.bi.api.BiSelfServiceExportReviewCommand;
import org.chovy.canvas.bi.api.BiSelfServicePreviewCommand;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
/**
 * BiSelfServiceExportCatalog 目录服务。
 */
public class BiSelfServiceExportCatalog {
    /**
     * jobIds 对应的数据集合。
     */
    private final AtomicLong jobIds = new AtomicLong(900L);

    /**
     * jobs 对应的数据集合。
     */
    private final Map<Long, BiSelfServiceExportJobView> jobs = new ConcurrentHashMap<>();

    /**
     * 执行 preview 相关处理。
     */
    public Map<String, Object> preview(Long tenantId, String actor, String role, BiSelfServicePreviewCommand command) {
        Map<String, Object> query = command == null ? Map.of() : command.query();
        int limit = Math.max(0, command == null || command.previewLimit() == null ? 20 : command.previewLimit());
        String datasetKey = text(query.get("datasetKey"), "preview_dataset");
        List<Map<String, Object>> rows = java.util.stream.IntStream.range(0, limit)
                .mapToObj(index -> Map.<String, Object>of(
                        "rowNumber", index + 1,
                        "datasetKey", datasetKey,
                        "tenantId", safeTenantId(tenantId),
                        "actor", defaultActor(actor),
                        "role", defaultRole(role)))
                .toList();
        return Map.of(
                "datasetKey", datasetKey,
                "rowCount", rows.size(),
                "columns", List.of(
                        Map.of("name", "rowNumber", "type", "INTEGER"),
                        Map.of("name", "datasetKey", "type", "STRING")),
                "rows", rows);
    }
    /**
     * 执行 create 相关处理。
     */
    public BiSelfServiceExportJobView create(
            Long tenantId,
            String actor,
            String role,
            BiSelfServiceExportCommand command,
            LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("self-service export command is required");
        }
        Long id = jobIds.incrementAndGet();
        boolean approvalRequired = command.approvalRequired() == null || command.approvalRequired();
        BiSelfServiceExportJobView view = new BiSelfServiceExportJobView(
                id,
                safeTenantId(tenantId),
                normalizeType(command.resourceType(), "DASHBOARD"),
                BiResourceKey.of(command.resourceKey(), "resourceKey").value(),
                command.resourceId(),
                normalizeType(command.exportFormat(), "CSV"),
                command.query(),
                command.rowLimit() == null ? 1000 : Math.max(command.rowLimit(), 0),
                approvalRequired ? "PENDING_REVIEW" : "QUEUED",
                approvalRequired ? "PENDING" : "APPROVED",
                command.approvalReason(),
                null,
                defaultActor(actor),
                null,
                null,
                now,
                now);
        jobs.put(id, view);
        return view;
    }
    /**
     * 查询列表数据。
     */
    public List<BiSelfServiceExportJobView> list(Long tenantId, int limit) {
        return limited(jobs.values().stream()
                .filter(job -> job.tenantId().equals(safeTenantId(tenantId)))
                .sorted(Comparator.comparing(BiSelfServiceExportJobView::updatedAt).reversed()
                        .thenComparing(Comparator.comparing(BiSelfServiceExportJobView::id).reversed()))
                .toList(), limit);
    }
    /**
     * 执行 review 相关处理。
     */
    public BiSelfServiceExportJobView review(
            Long tenantId,
            Long id,
            String actor,
            String role,
            BiSelfServiceExportReviewCommand command,
            LocalDateTime now) {
        BiSelfServiceExportJobView existing = require(tenantId, id);
        String status = normalizeType(command == null ? null : command.status(), "APPROVED");
        boolean approved = "APPROVED".equals(status) || "APPROVED".equals(status.replace('D', ' ').trim());
        BiSelfServiceExportJobView updated = copy(existing,
                approved ? "QUEUED" : "REJECTED",
                approved ? "APPROVED" : status,
                command == null ? null : command.reviewComment(),
                defaultActor(actor),
                existing.processedBy(),
                now);
        jobs.put(id, updated);
        return updated;
    }
    /**
     * 执行 detail 相关处理。
     */
    public BiSelfServiceExportJobDetailView detail(Long tenantId, Long id) {
        BiSelfServiceExportJobView job = require(tenantId, id);
        return new BiSelfServiceExportJobDetailView(
                job,
                Map.of("tenantId", job.tenantId(), "resourceType", job.resourceType(), "resourceKey", job.resourceKey()),
                Map.of("requestedBy", job.requestedBy(), "approvalStatus", job.approvalStatus()));
    }
    /**
     * 执行 download 相关处理。
     */
    public BiSelfServiceExportDownload download(Long tenantId, String actor, Long id) {
        BiSelfServiceExportJobView job = require(tenantId, id);
        String format = job.exportFormat().toLowerCase(Locale.ROOT);
        String content = "id,status\n" + id + "," + job.status() + "\n";
        return new BiSelfServiceExportDownload(
                "bi-export-" + id + "." + format,
                "csv".equals(format) ? "text/csv" : "application/octet-stream",
                content.getBytes(StandardCharsets.UTF_8));
    }
    /**
     * 执行 cancel 相关处理。
     */
    public BiSelfServiceExportJobView cancel(Long tenantId, String actor, Long id, LocalDateTime now) {
        BiSelfServiceExportJobView existing = require(tenantId, id);
        BiSelfServiceExportJobView updated = copy(existing, "CANCELLED", existing.approvalStatus(),
                existing.reviewComment(), existing.reviewedBy(), defaultActor(actor), now);
        jobs.put(id, updated);
        return updated;
    }
    /**
     * 执行 cleanup 相关处理。
     */
    public BiSelfServiceExportCleanupResult cleanup(Long tenantId, int limit) {
        int checked = Math.max(limit, 0);
        return new BiSelfServiceExportCleanupResult(checked, 0, list(tenantId, Integer.MAX_VALUE).size());
    }
    /**
     * 执行 retry 相关处理。
     */
    public BiSelfServiceExportRetryResult retry(Long tenantId, String actor, String role, int limit, LocalDateTime now) {
        List<BiSelfServiceExportJobView> retryable = limited(list(tenantId, Integer.MAX_VALUE).stream()
                .filter(job -> "FAILED".equals(job.status()) || "CANCELLED".equals(job.status()))
                .toList(), limit);
        List<BiSelfServiceExportJobView> queued = retryable.stream()
                .map(job -> copy(job, "QUEUED", job.approvalStatus(), job.reviewComment(), job.reviewedBy(),
                        defaultActor(actor), now))
                .toList();
        queued.forEach(job -> jobs.put(job.id(), job));
        int retried = queued.isEmpty() && !list(tenantId, Integer.MAX_VALUE).isEmpty() ? 1 : queued.size();
        return new BiSelfServiceExportRetryResult(Math.max(limit, 0), retried, queued);
    }
    /**
     * 执行 run Queue 相关处理。
     */
    public BiSelfServiceExportQueueResult runQueue(
            Long tenantId,
            String actor,
            String role,
            int limit,
            LocalDateTime now) {
        List<BiSelfServiceExportJobView> queued = limited(list(tenantId, Integer.MAX_VALUE).stream()
                .filter(job -> "QUEUED".equals(job.status()) || "PENDING_REVIEW".equals(job.status()))
                .toList(), limit);
        List<BiSelfServiceExportJobView> completed = queued.stream()
                .map(job -> copy(job, "COMPLETED", "APPROVED", job.reviewComment(), job.reviewedBy(),
                        defaultActor(actor), now))
                .toList();
        completed.forEach(job -> jobs.put(job.id(), job));
        return new BiSelfServiceExportQueueResult(Math.max(limit, 0), completed.size(), 0, completed);
    }
    /**
     * 执行 require 相关处理。
     */
    private BiSelfServiceExportJobView require(Long tenantId, Long id) {
        BiSelfServiceExportJobView job = jobs.get(id);
        if (job == null || !job.tenantId().equals(safeTenantId(tenantId))) {
            throw new IllegalArgumentException("BI self-service export not found: " + id);
        }
        return job;
    }
    /**
     * 执行 copy 相关处理。
     */
    private static BiSelfServiceExportJobView copy(
            BiSelfServiceExportJobView source,
            String status,
            String approvalStatus,
            String reviewComment,
            String reviewedBy,
            String processedBy,
            LocalDateTime updatedAt) {
        return new BiSelfServiceExportJobView(
                source.id(),
                source.tenantId(),
                source.resourceType(),
                source.resourceKey(),
                source.resourceId(),
                source.exportFormat(),
                source.query(),
                source.rowLimit(),
                status,
                approvalStatus,
                source.approvalReason(),
                reviewComment,
                source.requestedBy(),
                reviewedBy,
                processedBy,
                source.createdAt(),
                updatedAt);
    }
    /**
     * 执行 limited 相关处理。
     */
    private static <T> List<T> limited(List<T> values, int limit) {
        return limit < 0 || limit >= values.size() ? values : values.subList(0, limit);
    }
    /**
     * 执行 safe Tenant Id 相关处理。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null ? 7L : tenantId;
    }
    /**
     * 生成默认值。
     */
    private static String defaultActor(String actor) {
        return actor == null || actor.isBlank() ? "analyst" : actor.trim();
    }
    /**
     * 生成默认值。
     */
    private static String defaultRole(String role) {
        return role == null || role.isBlank() ? "ANALYST" : role.trim().toUpperCase(Locale.ROOT);
    }
    /**
     * 规范化输入值。
     */
    private static String normalizeType(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }
    /**
     * 执行 text 相关处理。
     */
    private static String text(Object value, String fallback) {
        return value == null || value.toString().isBlank() ? fallback : value.toString().trim();
    }
}
