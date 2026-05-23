package org.chovy.canvas.domain.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int TITLE_LIMIT = 200;
    private static final int CONTENT_LIMIT = 1000;
    private static final int TARGET_URL_LIMIT = 500;
    private static final int ACTION_LABEL_LIMIT = 64;
    private static final int BIZ_TYPE_LIMIT = 64;
    private static final int BIZ_ID_LIMIT = 128;
    private static final int DEDUP_KEY_LIMIT = 200;

    private final NotificationMapper mapper;
    private final NotificationRealtimePublisher realtimePublisher;

    public Notification createForTask(
            String userId, String type, String title, String content, String targetUrl, String taskId) {
        String severity = "TASK_FAILED".equals(type) ? "ERROR" : "SUCCESS";
        return create(NotificationCreateCommand.builder()
                .userId(userId)
                .category("TASK")
                .severity(severity)
                .type(type)
                .title(title)
                .content(content)
                .targetUrl(targetUrl)
                .actionLabel("查看结果")
                .actionUrl(targetUrl)
                .taskId(taskId)
                .bizType("ASYNC_TASK")
                .bizId(taskId)
                .dedupKey(taskId == null ? null : "task:" + taskId + ":" + type)
                .build());
    }

    public Notification create(NotificationCreateCommand command) {
        Notification notification = new Notification();
        notification.setNotificationId(newNotificationId());
        notification.setUserId(requireText(command.userId(), "userId"));
        notification.setType(requireText(command.type(), "type"));
        notification.setCategory(defaultIfBlank(command.category(), "TASK"));
        notification.setSeverity(defaultIfBlank(command.severity(), "INFO"));
        notification.setStatus("UNREAD");
        notification.setTitle(trimToLimit(requireText(command.title(), "title"), TITLE_LIMIT));
        notification.setContent(trimToLimit(command.content(), CONTENT_LIMIT));
        notification.setTargetUrl(trimToLimit(command.targetUrl(), TARGET_URL_LIMIT));
        notification.setActionLabel(trimToLimit(command.actionLabel(), ACTION_LABEL_LIMIT));
        notification.setActionUrl(trimToLimit(defaultIfBlank(command.actionUrl(), command.targetUrl()), TARGET_URL_LIMIT));
        notification.setTaskId(command.taskId());
        notification.setBizType(trimToLimit(command.bizType(), BIZ_TYPE_LIMIT));
        notification.setBizId(trimToLimit(command.bizId(), BIZ_ID_LIMIT));
        notification.setDedupKey(trimToLimit(command.dedupKey(), DEDUP_KEY_LIMIT));
        notification.setPayloadJson(command.payloadJson());
        try {
            mapper.insert(notification);
        } catch (DuplicateKeyException e) {
            Notification existing = findExisting(notification);
            if (existing != null) {
                return existing;
            }
            throw e;
        }
        realtimePublisher.publish(
                "NOTIFICATION_CREATED",
                notification.getUserId(),
                notification,
                unreadCount(notification.getUserId()));
        return notification;
    }

    public List<Notification> list(String userId, boolean unreadOnly, int page, int size) {
        return list(userId, unreadOnly, null, false, page, size);
    }

    public List<Notification> list(
            String userId, boolean unreadOnly, String category, boolean archived, int page, int size) {
        return mapper.selectPage(new Page<>(page, size), baseUserQuery(userId, unreadOnly, archived)
                        .eq(hasText(category), Notification::getCategory, category)
                        .orderByDesc(Notification::getCreatedAt))
                .getRecords();
    }

    public long unreadCount(String userId) {
        Long count = mapper.selectCount(baseUserQuery(userId, true, false));
        return count == null ? 0L : count;
    }

    public void markRead(String userId, String notificationId) {
        Notification update = new Notification();
        update.setReadAt(LocalDateTime.now());
        update.setStatus("READ");
        mapper.update(update, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getNotificationId, notificationId)
                .isNull(Notification::getArchivedAt)
                .isNull(Notification::getReadAt));
        realtimePublisher.publish("NOTIFICATION_UPDATED", userId, null, unreadCount(userId));
    }

    public void markAllRead(String userId) {
        Notification update = new Notification();
        update.setReadAt(LocalDateTime.now());
        update.setStatus("READ");
        mapper.update(update, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .isNull(Notification::getArchivedAt)
                .isNull(Notification::getReadAt));
        realtimePublisher.publish("NOTIFICATION_UPDATED", userId, null, unreadCount(userId));
    }

    public void archive(String userId, String notificationId) {
        Notification update = new Notification();
        update.setArchivedAt(LocalDateTime.now());
        update.setStatus("ARCHIVED");
        mapper.update(update, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getNotificationId, notificationId)
                .isNull(Notification::getArchivedAt));
        realtimePublisher.publish("NOTIFICATION_UPDATED", userId, null, unreadCount(userId));
    }

    private LambdaQueryWrapper<Notification> baseUserQuery(String userId, boolean unreadOnly, boolean archived) {
        LambdaQueryWrapper<Notification> query = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId);
        if (archived) {
            query.isNotNull(Notification::getArchivedAt);
        } else {
            query.isNull(Notification::getArchivedAt);
        }
        if (unreadOnly) {
            query.isNull(Notification::getReadAt);
        }
        return query;
    }

    private String newNotificationId() {
        return "ntf_" + UUID.randomUUID().toString().replace("-", "");
    }

    private Notification findExisting(Notification notification) {
        if (hasText(notification.getDedupKey())) {
            Notification existing = mapper.selectOne(new LambdaQueryWrapper<Notification>()
                    .eq(Notification::getUserId, notification.getUserId())
                    .eq(Notification::getDedupKey, notification.getDedupKey())
                    .last("LIMIT 1"));
            if (existing != null) {
                return existing;
            }
        }
        if (!hasText(notification.getTaskId())) {
            return null;
        }
        return mapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, notification.getUserId())
                .eq(Notification::getType, notification.getType())
                .eq(Notification::getTaskId, notification.getTaskId())
                .last("LIMIT 1"));
    }

    private String trimToLimit(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }

    private String defaultIfBlank(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private String requireText(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
