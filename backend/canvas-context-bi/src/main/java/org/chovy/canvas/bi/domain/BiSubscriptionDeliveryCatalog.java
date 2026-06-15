package org.chovy.canvas.bi.domain;

import org.chovy.canvas.bi.api.BiAlertRuleCommand;
import org.chovy.canvas.bi.api.BiAlertRuleView;
import org.chovy.canvas.bi.api.BiDeliveryAttachmentCleanupResult;
import org.chovy.canvas.bi.api.BiDeliveryAttachmentDownload;
import org.chovy.canvas.bi.api.BiDeliveryAttachmentView;
import org.chovy.canvas.bi.api.BiDeliveryAuditSummary;
import org.chovy.canvas.bi.api.BiDeliveryLogView;
import org.chovy.canvas.bi.api.BiDeliveryRetryResult;
import org.chovy.canvas.bi.api.BiDeliveryRunResult;
import org.chovy.canvas.bi.api.BiDeliverySchedulerResult;
import org.chovy.canvas.bi.api.BiSubscriptionCommand;
import org.chovy.canvas.bi.api.BiSubscriptionView;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class BiSubscriptionDeliveryCatalog {

    private static final Long DEFAULT_WORKSPACE_ID = 0L;
    private static final int DEFAULT_RETENTION_DAYS = 7;

    private final AtomicLong subscriptionIds = new AtomicLong(100L);
    private final AtomicLong alertIds = new AtomicLong(200L);
    private final AtomicLong logIds = new AtomicLong(300L);
    private final AtomicLong attachmentIds = new AtomicLong(400L);
    private final Map<Long, BiSubscriptionView> subscriptions = new ConcurrentHashMap<>();
    private final Map<Long, BiAlertRuleView> alerts = new ConcurrentHashMap<>();
    private final Map<Long, BiDeliveryLogView> logs = new ConcurrentHashMap<>();
    private final Map<Long, BiDeliveryAttachmentView> attachments = new ConcurrentHashMap<>();
    private final Map<Long, byte[]> attachmentBytes = new ConcurrentHashMap<>();

    public BiSubscriptionView upsertSubscription(Long tenantId, BiSubscriptionCommand command, String actor,
                                                 LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("subscription command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        String key = BiResourceKey.of(command.subscriptionKey(), "subscriptionKey").value();
        BiSubscriptionView existing = subscriptions.values().stream()
                .filter(view -> view.tenantId().equals(scopedTenantId))
                .filter(view -> view.subscriptionKey().equals(key))
                .findFirst()
                .orElse(null);
        Long id = existing == null ? subscriptionIds.incrementAndGet() : existing.id();
        LocalDateTime createdAt = existing == null ? now : existing.createdAt();
        String createdBy = existing == null ? defaultActor(actor) : existing.createdBy();
        BiSubscriptionView view = new BiSubscriptionView(
                id,
                scopedTenantId,
                DEFAULT_WORKSPACE_ID,
                key,
                textOrDefault(command.name(), key),
                normalizeType(command.resourceType(), "resourceType"),
                BiResourceKey.of(command.resourceKey(), "resourceKey").value(),
                command.resourceId(),
                command.schedule(),
                command.receivers(),
                command.delivery(),
                command.enabled() == null || command.enabled(),
                createdBy,
                createdAt,
                now);
        subscriptions.put(id, view);
        return view;
    }

    public List<BiSubscriptionView> listSubscriptions(Long tenantId, int limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        return limited(subscriptions.values().stream()
                .filter(view -> view.tenantId().equals(scopedTenantId))
                .sorted(Comparator.comparing(BiSubscriptionView::updatedAt).reversed()
                        .thenComparing(Comparator.comparing(BiSubscriptionView::id).reversed()))
                .toList(), limit);
    }

    public void deleteSubscription(Long tenantId, Long id) {
        removeScoped(subscriptions, safeTenantId(tenantId), id);
    }

    public BiAlertRuleView upsertAlert(Long tenantId, BiAlertRuleCommand command, String actor, LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("alert command is required");
        }
        Long scopedTenantId = safeTenantId(tenantId);
        String key = BiResourceKey.of(command.alertKey(), "alertKey").value();
        BiAlertRuleView existing = alerts.values().stream()
                .filter(view -> view.tenantId().equals(scopedTenantId))
                .filter(view -> view.alertKey().equals(key))
                .findFirst()
                .orElse(null);
        Long id = existing == null ? alertIds.incrementAndGet() : existing.id();
        LocalDateTime createdAt = existing == null ? now : existing.createdAt();
        String createdBy = existing == null ? defaultActor(actor) : existing.createdBy();
        BiAlertRuleView view = new BiAlertRuleView(
                id,
                scopedTenantId,
                DEFAULT_WORKSPACE_ID,
                key,
                textOrDefault(command.name(), key),
                BiResourceKey.of(command.datasetKey(), "datasetKey").value(),
                null,
                BiResourceKey.of(command.metricKey(), "metricKey").value(),
                command.condition(),
                command.receivers(),
                command.enabled() == null || command.enabled(),
                createdBy,
                createdAt,
                now);
        alerts.put(id, view);
        return view;
    }

    public List<BiAlertRuleView> listAlerts(Long tenantId, int limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        return limited(alerts.values().stream()
                .filter(view -> view.tenantId().equals(scopedTenantId))
                .sorted(Comparator.comparing(BiAlertRuleView::updatedAt).reversed()
                        .thenComparing(Comparator.comparing(BiAlertRuleView::id).reversed()))
                .toList(), limit);
    }

    public void deleteAlert(Long tenantId, Long id) {
        removeScoped(alerts, safeTenantId(tenantId), id);
    }

    public BiDeliveryRunResult runSubscription(Long tenantId, Long id, String actor, LocalDateTime now) {
        BiSubscriptionView subscription = subscriptions.get(id);
        if (subscription == null || !subscription.tenantId().equals(safeTenantId(tenantId))) {
            return new BiDeliveryRunResult("SUBSCRIPTION", id, "subscription-" + id, "SKIPPED", List.of());
        }
        BiDeliveryLogView log = appendLog(subscription.tenantId(), "SUBSCRIPTION", id, subscription.subscriptionKey(),
                subscription.resourceType(), subscription.resourceId(), "TRIGGERED", "Subscription delivery triggered",
                actor, now);
        appendAttachment(subscription.tenantId(), "SUBSCRIPTION", id, subscription.subscriptionKey(), log.id(),
                subscription.resourceType(), subscription.resourceId(), "subscription-" + id,
                subscription.subscriptionKey() + "-delivery.txt", actor, now);
        return new BiDeliveryRunResult("SUBSCRIPTION", id, subscription.subscriptionKey(), "TRIGGERED", List.of(log));
    }

    public BiDeliveryRunResult runAlert(Long tenantId, Long id, String actor, LocalDateTime now) {
        BiAlertRuleView alert = alerts.get(id);
        if (alert == null || !alert.tenantId().equals(safeTenantId(tenantId))) {
            return new BiDeliveryRunResult("ALERT", id, "alert-" + id, "SKIPPED", List.of());
        }
        BiDeliveryLogView log = appendLog(alert.tenantId(), "ALERT", id, alert.alertKey(),
                "ALERT", alert.datasetId(), "TRIGGERED", "Alert delivery triggered", actor, now);
        appendAttachment(alert.tenantId(), "ALERT", id, alert.alertKey(), log.id(), "ALERT", alert.datasetId(),
                "alert-" + id, alert.alertKey() + "-delivery.txt", actor, now);
        return new BiDeliveryRunResult("ALERT", id, alert.alertKey(), "TRIGGERED", List.of(log));
    }

    public List<BiDeliveryLogView> listLogs(Long tenantId, String jobType, Long jobId, int limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedJobType = normalizeNullableType(jobType);
        return limited(logs.values().stream()
                .filter(log -> log.tenantId().equals(scopedTenantId))
                .filter(log -> normalizedJobType == null || log.jobType().equals(normalizedJobType))
                .filter(log -> jobId == null || log.jobId().equals(jobId))
                .sorted(logOrdering())
                .toList(), limit);
    }

    public BiDeliveryAuditSummary audit(Long tenantId, String jobType, String status, String channel, Long jobId,
                                        int limit) {
        List<BiDeliveryLogView> matching = listLogs(tenantId, jobType, jobId, Integer.MAX_VALUE).stream()
                .filter(log -> status == null || status.isBlank() || log.status().equals(normalizeType(status, "status")))
                .filter(log -> channel == null || channel.isBlank() || log.channel().equals(normalizeType(channel, "channel")))
                .toList();
        return new BiDeliveryAuditSummary(
                matching.size(),
                countStatus(matching, "DELIVERED"),
                countStatus(matching, "TRIGGERED"),
                countStatus(matching, "SKIPPED"),
                countStatus(matching, "PENDING"),
                countStatus(matching, "FAILED"),
                (int) matching.stream().filter(log -> log.retryCount() < log.maxRetryCount()).count(),
                (int) matching.stream().filter(log -> log.retryExhaustedAt() != null).count(),
                limited(matching, limit));
    }

    public BiDeliveryRetryResult retry(Long tenantId, String actor, int limit, LocalDateTime now) {
        List<BiDeliveryLogView> retryable = limited(listLogs(tenantId, null, null, Integer.MAX_VALUE).stream()
                .filter(log -> "TRIGGERED".equals(log.status()) || "PENDING".equals(log.status()) || "FAILED".equals(log.status()))
                .toList(), limit);
        List<BiDeliveryLogView> delivered = retryable.stream()
                .map(log -> appendLog(log.tenantId(), log.jobType(), log.jobId(), log.jobKey(), log.resourceType(),
                        log.resourceId(), "DELIVERED", "Delivery retry completed", actor, now))
                .toList();
        return new BiDeliveryRetryResult(
                retryable.size(),
                delivered.size(),
                delivered.size(),
                0,
                0,
                delivered);
    }

    public List<BiDeliveryAttachmentView> listAttachments(Long tenantId, String jobType, Long jobId,
                                                          Long deliveryLogId, int limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        String normalizedJobType = normalizeNullableType(jobType);
        return limited(attachments.values().stream()
                .filter(attachment -> attachment.tenantId().equals(scopedTenantId))
                .filter(attachment -> normalizedJobType == null || attachment.jobType().equals(normalizedJobType))
                .filter(attachment -> jobId == null || attachment.jobId().equals(jobId))
                .filter(attachment -> deliveryLogId == null || attachment.deliveryLogId().equals(deliveryLogId))
                .filter(attachment -> !"DELETED".equals(attachment.status()))
                .sorted(attachmentOrdering())
                .toList(), limit);
    }

    public BiDeliveryAttachmentDownload download(Long tenantId, Long id, String actor, LocalDateTime now) {
        BiDeliveryAttachmentView attachment = attachments.get(id);
        if (attachment == null || !attachment.tenantId().equals(safeTenantId(tenantId))) {
            throw new IllegalArgumentException("BI delivery attachment not found");
        }
        BiDeliveryAttachmentView updated = new BiDeliveryAttachmentView(
                attachment.id(), attachment.tenantId(), attachment.workspaceId(), attachment.jobType(),
                attachment.jobId(), attachment.jobKey(), attachment.deliveryLogId(), attachment.resourceType(),
                attachment.resourceId(), attachment.attachmentKey(), attachment.attachmentType(),
                attachment.fileName(), attachment.contentType(), attachment.fileUrl(), attachment.storageProvider(),
                attachment.storageKey(), attachment.sizeBytes(), attachment.retentionDays(), attachment.expiresAt(),
                attachment.downloadCount() + 1, now, attachment.status(), attachment.errorMessage(),
                attachment.createdBy(), attachment.createdAt(), now);
        attachments.put(id, updated);
        return new BiDeliveryAttachmentDownload(
                attachment.fileName(),
                attachment.contentType(),
                attachmentBytes.getOrDefault(id, new byte[0]));
    }

    public BiDeliveryAttachmentCleanupResult cleanup(Long tenantId, int limit) {
        List<BiDeliveryAttachmentView> scoped = limited(listAttachments(tenantId, null, null, null, Integer.MAX_VALUE),
                limit);
        scoped.forEach(attachment -> {
            attachments.put(attachment.id(), new BiDeliveryAttachmentView(
                    attachment.id(), attachment.tenantId(), attachment.workspaceId(), attachment.jobType(),
                    attachment.jobId(), attachment.jobKey(), attachment.deliveryLogId(), attachment.resourceType(),
                    attachment.resourceId(), attachment.attachmentKey(), attachment.attachmentType(),
                    attachment.fileName(), attachment.contentType(), attachment.fileUrl(), attachment.storageProvider(),
                    attachment.storageKey(), attachment.sizeBytes(), attachment.retentionDays(), attachment.expiresAt(),
                    attachment.downloadCount(), attachment.lastDownloadedAt(), "DELETED", attachment.errorMessage(),
                    attachment.createdBy(), attachment.createdAt(), attachment.updatedAt()));
            attachmentBytes.remove(attachment.id());
        });
        return new BiDeliveryAttachmentCleanupResult(scoped.size(), scoped.size(), scoped.size(), 0);
    }

    public BiDeliverySchedulerResult runScheduler(Long tenantId, String actor, LocalDateTime now) {
        Long scopedTenantId = safeTenantId(tenantId);
        List<BiSubscriptionView> dueSubscriptions = subscriptions.values().stream()
                .filter(view -> view.tenantId().equals(scopedTenantId))
                .filter(BiSubscriptionView::enabled)
                .toList();
        List<BiAlertRuleView> dueAlerts = alerts.values().stream()
                .filter(view -> view.tenantId().equals(scopedTenantId))
                .filter(BiAlertRuleView::enabled)
                .toList();
        dueSubscriptions.forEach(view -> runSubscription(scopedTenantId, view.id(), actor, now));
        dueAlerts.forEach(view -> runAlert(scopedTenantId, view.id(), actor, now));
        return new BiDeliverySchedulerResult(
                dueSubscriptions.size(),
                dueSubscriptions.size(),
                dueAlerts.size(),
                dueAlerts.size(),
                0,
                0);
    }

    private BiDeliveryLogView appendLog(Long tenantId, String jobType, Long jobId, String jobKey, String resourceType,
                                        Long resourceId, String status, String message, String actor,
                                        LocalDateTime now) {
        Long id = logIds.incrementAndGet();
        BiDeliveryLogView log = new BiDeliveryLogView(
                id,
                tenantId,
                DEFAULT_WORKSPACE_ID,
                normalizeType(jobType, "jobType"),
                jobId,
                jobKey,
                normalizeType(resourceType, "resourceType"),
                resourceId,
                "EMAIL",
                Map.of(),
                Map.of("jobKey", jobKey),
                BigDecimal.ZERO,
                normalizeType(status, "status"),
                message,
                null,
                0,
                3,
                null,
                null,
                null,
                defaultActor(actor),
                now.plusNanos(id),
                now.plusNanos(id));
        logs.put(id, log);
        return log;
    }

    private void appendAttachment(Long tenantId, String jobType, Long jobId, String jobKey, Long deliveryLogId,
                                  String resourceType, Long resourceId, String attachmentKey, String fileName,
                                  String actor, LocalDateTime now) {
        Long id = attachmentIds.incrementAndGet();
        byte[] bytes = ("delivery-" + id).getBytes(StandardCharsets.UTF_8);
        BiDeliveryAttachmentView attachment = new BiDeliveryAttachmentView(
                id,
                tenantId,
                DEFAULT_WORKSPACE_ID,
                normalizeType(jobType, "jobType"),
                jobId,
                jobKey,
                deliveryLogId,
                normalizeType(resourceType, "resourceType"),
                resourceId,
                attachmentKey,
                "TEXT",
                fileName,
                "text/plain",
                "/canvas/bi/delivery-attachments/" + id + "/download",
                "MEMORY",
                "bi-delivery/" + id,
                (long) bytes.length,
                DEFAULT_RETENTION_DAYS,
                now.plusDays(DEFAULT_RETENTION_DAYS),
                0,
                null,
                "AVAILABLE",
                null,
                defaultActor(actor),
                now.plusNanos(id),
                now.plusNanos(id));
        attachments.put(id, attachment);
        attachmentBytes.put(id, bytes);
    }

    private static <T> void removeScoped(Map<Long, T> values, Long tenantId, Long id) {
        Object value = values.get(id);
        if (value instanceof BiSubscriptionView view && view.tenantId().equals(tenantId)) {
            values.remove(id);
        }
        if (value instanceof BiAlertRuleView view && view.tenantId().equals(tenantId)) {
            values.remove(id);
        }
    }

    private static int countStatus(List<BiDeliveryLogView> values, String status) {
        return (int) values.stream().filter(log -> status.equals(log.status())).count();
    }

    private static Comparator<BiDeliveryLogView> logOrdering() {
        return Comparator.comparing(BiDeliveryLogView::createdAt).reversed()
                .thenComparing(Comparator.comparing(BiDeliveryLogView::id).reversed());
    }

    private static Comparator<BiDeliveryAttachmentView> attachmentOrdering() {
        return Comparator.comparing(BiDeliveryAttachmentView::createdAt).reversed()
                .thenComparing(Comparator.comparing(BiDeliveryAttachmentView::id).reversed());
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

    private static String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String normalizeNullableType(String value) {
        return value == null || value.isBlank() ? null : normalizeType(value, "type");
    }

    private static String normalizeType(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return normalized;
    }
}
