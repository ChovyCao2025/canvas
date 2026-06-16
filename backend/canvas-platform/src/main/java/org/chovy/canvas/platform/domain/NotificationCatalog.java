package org.chovy.canvas.platform.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 保存通知演示数据并提供通知查询、已读、归档和票据操作。
 */
public class NotificationCatalog {

    /**
     * WebSocket 通知票据有效期，单位为秒。
     */
    private static final int WS_TICKET_TTL_SECONDS = 60;

    /**
     * 内存中的通知记录列表。
     */
    private final List<Map<String, Object>> notifications = new ArrayList<>();

    /**
     * 创建通知目录并写入固定演示数据。
     */
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

    /**
     * 分页查询指定用户的通知。
     *
     * @param tenantId 租户标识
     * @param actor 用户标识
     * @param unreadOnly 是否只查询未读通知
     * @param archived 是否查询已归档通知
     * @param category 通知分类过滤值
     * @param page 页码
     * @param size 每页数量
     * @return 通知视图列表
     */
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

    /**
     * 统计指定用户未读通知数量。
     *
     * @param tenantId 租户标识
     * @param actor 用户标识
     * @return 包含 count 字段的统计结果
     */
    public Map<String, Object> unreadCount(Long tenantId, String actor) {
        long count = notifications.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("userId"), actor))
                .filter(row -> row.get("archivedAt") == null)
                .filter(row -> row.get("readAt") == null)
                .count();
        return ordered("count", count);
    }

    /**
     * 将单条通知标记为已读。
     *
     * @param tenantId 租户标识
     * @param actor 用户标识
     * @param notificationId 通知标识
     */
    public void markRead(Long tenantId, String actor, String notificationId) {
        notifications.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("userId"), actor))
                .filter(row -> Objects.equals(row.get("notificationId"), notificationId))
                .filter(row -> row.get("archivedAt") == null)
                .filter(row -> row.get("readAt") == null)
                .findFirst()
                .ifPresent(row -> {
                    // 已读时间固定，保证本地演示和测试快照稳定。
                    row.put("status", "READ");
                    row.put("readAt", "2026-01-01T00:10:00Z");
                });
    }

    /**
     * 将用户所有未归档未读通知标记为已读。
     *
     * @param tenantId 租户标识
     * @param actor 用户标识
     */
    public void markAllRead(Long tenantId, String actor) {
        notifications.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("userId"), actor))
                .filter(row -> row.get("archivedAt") == null)
                .filter(row -> row.get("readAt") == null)
                .forEach(row -> {
                    // 批量已读使用同一固定时间，便于前端按状态验证而不受当前时间影响。
                    row.put("status", "READ");
                    row.put("readAt", "2026-01-01T00:10:00Z");
                });
    }

    /**
     * 归档单条通知。
     *
     * @param tenantId 租户标识
     * @param actor 用户标识
     * @param notificationId 通知标识
     */
    public void archive(Long tenantId, String actor, String notificationId) {
        notifications.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("userId"), actor))
                .filter(row -> Objects.equals(row.get("notificationId"), notificationId))
                .filter(row -> row.get("archivedAt") == null)
                .findFirst()
                .ifPresent(row -> {
                    // 归档时间固定，避免演示数据在重复运行时产生漂移。
                    row.put("status", "ARCHIVED");
                    row.put("archivedAt", "2026-01-01T00:11:00Z");
                });
    }

    /**
     * 创建通知 WebSocket 订阅票据。
     *
     * @param tenantId 租户标识
     * @param actor 用户标识
     * @return 票据记录
     */
    public Map<String, Object> createWsTicket(Long tenantId, String actor) {
        return ordered(
                "ticket", "ntf_ws_" + UUID.randomUUID().toString().replace("-", ""),
                "expiresInSeconds", WS_TICKET_TTL_SECONDS,
                "tenantId", tenantId,
                "userId", actor);
    }

    /**
     * 构造一条通知记录。
     *
     * @param notificationId 通知标识
     * @param tenantId 租户标识
     * @param userId 用户标识
     * @param type 通知类型
     * @param category 通知分类
     * @param severity 严重级别
     * @param status 通知状态
     * @param title 通知标题
     * @param content 通知正文
     * @param targetUrl 目标地址
     * @param actionLabel 动作按钮文案
     * @param actionUrl 动作地址
     * @param taskId 关联任务标识
     * @param bizType 业务类型
     * @param bizId 业务标识
     * @param dedupKey 去重键
     * @param payloadJson 业务负载 JSON
     * @param readAt 已读时间
     * @param archivedAt 归档时间
     * @param deliveredAt 送达时间
     * @param createdAt 创建时间
     * @return 通知记录
     */
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

    /**
     * 复制通知并移除内部租户和用户字段。
     *
     * @param source 内部通知记录
     * @return 外部可见通知记录
     */
    private static Map<String, Object> externalCopy(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.remove("tenantId");
        copy.remove("userId");
        return copy;
    }

    /**
     * 按参数顺序构造有序 Map。
     *
     * @param pairs 键值交替排列的参数
     * @return 有序 Map
     */
    private static Map<String, Object> ordered(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return result;
    }
}
