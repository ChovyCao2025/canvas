package org.chovy.canvas.domain.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper mapper;

    public Notification createForTask(
            String userId, String type, String title, String content, String targetUrl, String taskId) {
        Notification notification = new Notification();
        notification.setNotificationId(newNotificationId());
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetUrl(targetUrl);
        notification.setTaskId(taskId);
        mapper.insert(notification);
        return notification;
    }

    public Page<Notification> list(String userId, boolean unreadOnly, int page, int size) {
        return mapper.selectPage(new Page<>(page, size), baseUserQuery(userId, unreadOnly)
                .orderByDesc(Notification::getCreatedAt));
    }

    public long unreadCount(String userId) {
        Long count = mapper.selectCount(baseUserQuery(userId, true));
        return count == null ? 0L : count;
    }

    public void markRead(String userId, String notificationId) {
        Notification notification = mapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getNotificationId, notificationId)
                .last("LIMIT 1"));
        if (notification == null || notification.getReadAt() != null) {
            return;
        }

        notification.setReadAt(LocalDateTime.now());
        mapper.updateById(notification);
    }

    public void markAllRead(String userId) {
        List<Notification> notifications = mapper.selectList(baseUserQuery(userId, true));
        if (notifications.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Notification notification : notifications) {
            notification.setReadAt(now);
            mapper.updateById(notification);
        }
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
}
