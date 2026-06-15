package org.chovy.canvas.platform.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class NotificationCatalog {

    private static final int WS_TICKET_TTL_SECONDS = 60;

    private final List<Map<String, Object>> notifications = new ArrayList<>();

    public NotificationCatalog() {
        notifications.add(notification("ntf_task_failed_001", 7L, "operator-1", "TASK_FAILED", "TASK", "ERROR",
                "UNREAD", "Import failed", "Tag import task failed", "/tasks/task-1001", "查看结果",
                "/tasks/task-1001", "task-1001", "ASYNC_TASK", "task-1001",
                "task:task-1001:TASK_FAILED", "{\"taskId\":\"task-1001\"}", null, null,
                "2026-01-01T00:00:10Z", "2026-01-01T00:00:00Z"));
        notifications.add(notification("ntf_canvas_001", 7L, "operator-1", "CANVAS_PUBLISHED", "CANVAS", "INFO",
                "UNREAD", "Canvas published", "Welcome Journey is online", "/canvas/3001", "查看画布",
                "/canvas/3001", null, "CANVAS", "3001", "canvas:3001:published", "{\"canvasId\":3001}",
                null, null, "2026-01-01T00:01:10Z", "2026-01-01T00:01:00Z"));
        notifications.add(notification("ntf_archived_001", 7L, "operator-1", "SYSTEM_NOTICE", "SYSTEM", "INFO",
                "ARCHIVED", "Archived notice", "Already archived", null, null, null, null, "SYSTEM", "notice-1",
                "system:notice-1", "{}", "2026-01-01T00:02:30Z", "2026-01-01T00:03:00Z", null,
                "2026-01-01T00:02:00Z"));
        notifications.add(notification("ntf_other_user_001", 7L, "operator-2", "TASK_SUCCEEDED", "TASK", "SUCCESS",
                "UNREAD", "Other user task", "Task finished", "/tasks/task-2001", "查看结果", "/tasks/task-2001",
                "task-2001", "ASYNC_TASK", "task-2001", "task:task-2001:TASK_SUCCEEDED", "{}",
                null, null, null, "2026-01-01T00:04:00Z"));
        notifications.add(notification("ntf_tenant8_001", 8L, "operator-1", "SYSTEM_NOTICE", "SYSTEM", "INFO",
                "UNREAD", "Tenant 8 notice", "Tenant scoped", null, null, null, null, "SYSTEM", "tenant-8",
                "system:tenant-8", "{}", null, null, null, "2026-01-01T00:05:00Z"));
    }

    public List<Map<String, Object>> list(Long tenantId, String actor, boolean unreadOnly, boolean archived,
                                          String category, int page, int size) {
        int offset = (page - 1) * size;
        return notifications.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("userId"), actor))
                .filter(row -> category == null || category.equals(row.get("category")))
                .filter(row -> archived == (row.get("archivedAt") != null))
                .filter(row -> !unreadOnly || row.get("readAt") == null)
                .sorted(Comparator.comparing(row -> Instant.parse(String.valueOf(row.get("createdAt"))),
                        Comparator.reverseOrder()))
                .skip(offset)
                .limit(size)
                .map(NotificationCatalog::externalCopy)
                .toList();
    }

    public Map<String, Object> unreadCount(Long tenantId, String actor) {
        long count = notifications.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("userId"), actor))
                .filter(row -> row.get("archivedAt") == null)
                .filter(row -> row.get("readAt") == null)
                .count();
        return ordered("count", count);
    }

    public void markRead(Long tenantId, String actor, String notificationId) {
        notifications.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("userId"), actor))
                .filter(row -> Objects.equals(row.get("notificationId"), notificationId))
                .filter(row -> row.get("archivedAt") == null)
                .filter(row -> row.get("readAt") == null)
                .findFirst()
                .ifPresent(row -> {
                    row.put("status", "READ");
                    row.put("readAt", "2026-01-01T00:10:00Z");
                });
    }

    public void markAllRead(Long tenantId, String actor) {
        notifications.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("userId"), actor))
                .filter(row -> row.get("archivedAt") == null)
                .filter(row -> row.get("readAt") == null)
                .forEach(row -> {
                    row.put("status", "READ");
                    row.put("readAt", "2026-01-01T00:10:00Z");
                });
    }

    public void archive(Long tenantId, String actor, String notificationId) {
        notifications.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("userId"), actor))
                .filter(row -> Objects.equals(row.get("notificationId"), notificationId))
                .filter(row -> row.get("archivedAt") == null)
                .findFirst()
                .ifPresent(row -> {
                    row.put("status", "ARCHIVED");
                    row.put("archivedAt", "2026-01-01T00:11:00Z");
                });
    }

    public Map<String, Object> createWsTicket(Long tenantId, String actor) {
        return ordered(
                "ticket", "ntf_ws_" + UUID.randomUUID().toString().replace("-", ""),
                "expiresInSeconds", WS_TICKET_TTL_SECONDS,
                "tenantId", tenantId,
                "userId", actor);
    }

    private static Map<String, Object> notification(String notificationId, Long tenantId, String userId, String type,
                                                    String category, String severity, String status, String title,
                                                    String content, String targetUrl, String actionLabel,
                                                    String actionUrl, String taskId, String bizType, String bizId,
                                                    String dedupKey, String payloadJson, String readAt,
                                                    String archivedAt, String deliveredAt, String createdAt) {
        return ordered(
                "notificationId", notificationId,
                "tenantId", tenantId,
                "userId", userId,
                "type", type,
                "category", category,
                "severity", severity,
                "status", status,
                "title", title,
                "content", content,
                "targetUrl", targetUrl,
                "actionLabel", actionLabel,
                "actionUrl", actionUrl,
                "taskId", taskId,
                "bizType", bizType,
                "bizId", bizId,
                "dedupKey", dedupKey,
                "payloadJson", payloadJson,
                "readAt", readAt,
                "archivedAt", archivedAt,
                "deliveredAt", deliveredAt,
                "createdAt", createdAt);
    }

    private static Map<String, Object> externalCopy(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.remove("tenantId");
        copy.remove("userId");
        return copy;
    }

    private static Map<String, Object> ordered(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return result;
    }
}
