package org.chovy.canvas.domain.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
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

    private final NotificationMapper mapper;

    public Notification createForTask(
            String userId, String type, String title, String content, String targetUrl, String taskId) {
        Notification notification = new Notification();
        notification.setNotificationId(newNotificationId());
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(trimToLimit(title, TITLE_LIMIT));
        notification.setContent(trimToLimit(content, CONTENT_LIMIT));
        notification.setTargetUrl(trimToLimit(targetUrl, TARGET_URL_LIMIT));
        notification.setTaskId(taskId);
        mapper.insert(notification);
        return notification;
    }

    public List<Notification> list(String userId, boolean unreadOnly, int page, int size) {
        return mapper.selectPage(new Page<>(page, size), baseUserQuery(userId, unreadOnly)
                        .orderByDesc(Notification::getCreatedAt))
                .getRecords();
    }

    public long unreadCount(String userId) {
        Long count = mapper.selectCount(baseUserQuery(userId, true));
        return count == null ? 0L : count;
    }

    public void markRead(String userId, String notificationId) {
        Notification update = new Notification();
        update.setReadAt(LocalDateTime.now());
        mapper.update(update, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getNotificationId, notificationId)
                .isNull(Notification::getReadAt));
    }

    public void markAllRead(String userId) {
        Notification update = new Notification();
        update.setReadAt(LocalDateTime.now());
        mapper.update(update, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .isNull(Notification::getReadAt));
    }

    private LambdaQueryWrapper<Notification> baseUserQuery(String userId, boolean unreadOnly) {
        LambdaQueryWrapper<Notification> query = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId);
        if (unreadOnly) {
            query.isNull(Notification::getReadAt);
        }
        return query;
    }

    private String newNotificationId() {
        return "ntf_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String trimToLimit(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }
}
