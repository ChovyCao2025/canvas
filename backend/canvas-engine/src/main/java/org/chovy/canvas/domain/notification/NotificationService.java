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
import org.chovy.canvas.dal.dataobject.NotificationDO;
import org.chovy.canvas.dal.mapper.NotificationMapper;

/**
 * 站内通知持久化服务。
 *
 * <p>负责通知创建、查询、未读计数、已读和归档状态维护；持久化成功后触发实时推送。
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    /** 通知标题最大长度，避免数据库字段溢出。 */
    private static final int TITLE_LIMIT = 200;
    /** 通知正文最大长度，避免数据库字段溢出。 */
    private static final int CONTENT_LIMIT = 1000;
    /** 通知跳转地址最大长度，避免数据库字段溢出。 */
    private static final int TARGET_URL_LIMIT = 500;
    /** 通知操作按钮文案最大长度，避免数据库字段溢出。 */
    private static final int ACTION_LABEL_LIMIT = 64;
    /** 通知业务类型最大长度，避免数据库字段溢出。 */
    private static final int BIZ_TYPE_LIMIT = 64;
    /** 通知业务对象 ID 最大长度，避免数据库字段溢出。 */
    private static final int BIZ_ID_LIMIT = 128;
    /** 通知去重键最大长度，避免数据库字段溢出。 */
    private static final int DEDUP_KEY_LIMIT = 200;

    /** 通知 Mapper，用于通知创建、查询、已读和归档更新。 */
    private final NotificationMapper mapper;
    /** 实时通知发布器，用于把通知推送到 WebSocket 通道。 */
    private final NotificationRealtimePublisher realtimePublisher;

    /** 为异步任务创建通知。 */
    public NotificationDO createForTask(
            String userId, String type, String title, String content, String targetUrl, String taskId) {
        return createForTask(null, userId, type, title, content, targetUrl, taskId);
    }

    /** 为指定租户内的异步任务创建通知。 */
    public NotificationDO createForTask(
            Long tenantId, String userId, String type, String title, String content, String targetUrl, String taskId) {
        String severity = "TASK_FAILED".equals(type) ? "ERROR" : "SUCCESS";
        return create(NotificationCreateCommand.builder()
                .tenantId(tenantId)
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

    /** 创建新记录，并执行必要的唯一性、格式和默认值处理。 */
    public NotificationDO create(NotificationCreateCommand command) {
        NotificationDO notification = new NotificationDO();
        notification.setTenantId(command.tenantId());
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (DuplicateKeyException e) {
            // 通知去重命中时返回已存在消息，不再重复推送相同业务通知。
            NotificationDO existing = findExisting(notification);
            if (existing != null) {
                return existing;
            }
            throw e;
        }
        // 持久化成功后再推送实时消息，确保前端收到通知时列表接口也能查到。
        realtimePublisher.publish(
                "NOTIFICATION_CREATED",
                notification.getTenantId(),
                notification.getUserId(),
                notification,
                unreadCount(notification.getUserId(), notification.getTenantId()));
        return notification;
    }

    /** 按条件查询列表数据。 */
    public List<NotificationDO> list(String userId, boolean unreadOnly, int page, int size) {
        return list(userId, unreadOnly, null, false, page, size);
    }

    /** 按条件查询列表数据。 */
    public List<NotificationDO> list(
            String userId, boolean unreadOnly, String category, boolean archived, int page, int size) {
        return list(null, userId, unreadOnly, category, archived, page, size);
    }

    /** 按租户和条件查询列表数据。 */
    public List<NotificationDO> list(
            Long tenantId, String userId, boolean unreadOnly, String category, boolean archived, int page, int size) {
        return mapper.selectPage(new Page<>(page, size), baseUserQuery(tenantId, userId, unreadOnly, archived)
                        .eq(hasText(category), NotificationDO::getCategory, category)
                        .orderByDesc(NotificationDO::getCreatedAt))
                .getRecords();
    }

    /** 统计用户未读通知数量。 */
    public long unreadCount(String userId) {
        return unreadCount(userId, null);
    }

    /** 统计指定租户内用户未读通知数量。 */
    public long unreadCount(String userId, Long tenantId) {
        Long count = mapper.selectCount(baseUserQuery(tenantId, userId, true, false));
        return count == null ? 0L : count;
    }

    /** 将单条通知标记为已读。 */
    public void markRead(String userId, String notificationId) {
        markRead(userId, notificationId, null);
    }

    /** 将指定租户内单条通知标记为已读。 */
    public void markRead(String userId, String notificationId, Long tenantId) {
        NotificationDO update = new NotificationDO();
        update.setReadAt(LocalDateTime.now());
        update.setStatus("READ");
        mapper.update(update, userUpdate(tenantId)
                .eq(NotificationDO::getUserId, userId)
                .eq(NotificationDO::getNotificationId, notificationId)
                .isNull(NotificationDO::getArchivedAt)
                .isNull(NotificationDO::getReadAt));
        // 更新类通知只广播未读数，具体列表由前端按需重新拉取。
        realtimePublisher.publish("NOTIFICATION_UPDATED", tenantId, userId, null, unreadCount(userId, tenantId));
    }

    /** 将用户所有通知标记为已读。 */
    public void markAllRead(String userId) {
        markAllRead(userId, null);
    }

    /** 将指定租户内用户所有通知标记为已读。 */
    public void markAllRead(String userId, Long tenantId) {
        NotificationDO update = new NotificationDO();
        update.setReadAt(LocalDateTime.now());
        update.setStatus("READ");
        mapper.update(update, userUpdate(tenantId)
                .eq(NotificationDO::getUserId, userId)
                .isNull(NotificationDO::getArchivedAt)
                .isNull(NotificationDO::getReadAt));
        // 批量已读后推送最新未读数，避免多条单独消息刷屏。
        realtimePublisher.publish("NOTIFICATION_UPDATED", tenantId, userId, null, unreadCount(userId, tenantId));
    }

    /** 归档单条通知。 */
    public void archive(String userId, String notificationId) {
        archive(userId, notificationId, null);
    }

    /** 归档指定租户内单条通知。 */
    public void archive(String userId, String notificationId, Long tenantId) {
        NotificationDO update = new NotificationDO();
        update.setArchivedAt(LocalDateTime.now());
        update.setStatus("ARCHIVED");
        mapper.update(update, userUpdate(tenantId)
                .eq(NotificationDO::getUserId, userId)
                .eq(NotificationDO::getNotificationId, notificationId)
                .isNull(NotificationDO::getArchivedAt));
        // 归档会改变未读口径，实时事件用于刷新角标和列表。
        realtimePublisher.publish("NOTIFICATION_UPDATED", tenantId, userId, null, unreadCount(userId, tenantId));
    }

    /** 构建用户通知基础查询条件，统一处理归档和未读过滤。 */
    private LambdaQueryWrapper<NotificationDO> baseUserQuery(
            Long tenantId, String userId, boolean unreadOnly, boolean archived) {
        // 准备本次处理所需的上下文和中间变量。
        LambdaQueryWrapper<NotificationDO> query = new LambdaQueryWrapper<NotificationDO>()
                .eq(NotificationDO::getUserId, userId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (tenantId != null) {
            query.eq(NotificationDO::getTenantId, tenantId);
        }
        if (archived) {
            query.isNotNull(NotificationDO::getArchivedAt);
        } else {
            query.isNull(NotificationDO::getArchivedAt);
        }
        if (unreadOnly) {
            query.isNull(NotificationDO::getReadAt);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return query;
    }

    /** 生成通知业务 ID，避免暴露数据库自增主键。 */
    private String newNotificationId() {
        return "ntf_" + UUID.randomUUID().toString().replace("-", "");
    }

    /** 在唯一键冲突后按去重键或任务维度查询已有通知。 */
    private NotificationDO findExisting(NotificationDO notification) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(notification.getDedupKey())) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            NotificationDO existing = mapper.selectOne(new LambdaQueryWrapper<NotificationDO>()
                    .eq(notification.getTenantId() != null, NotificationDO::getTenantId, notification.getTenantId())
                    .eq(NotificationDO::getUserId, notification.getUserId())
                    .eq(NotificationDO::getDedupKey, notification.getDedupKey())
                    .last("LIMIT 1"));
            if (existing != null) {
                return existing;
            }
        }
        if (!hasText(notification.getTaskId())) {
            return null;
        }
        return mapper.selectOne(new LambdaQueryWrapper<NotificationDO>()
                .eq(notification.getTenantId() != null, NotificationDO::getTenantId, notification.getTenantId())
                .eq(NotificationDO::getUserId, notification.getUserId())
                .eq(NotificationDO::getType, notification.getType())
                .eq(NotificationDO::getTaskId, notification.getTaskId())
                .last("LIMIT 1"));
    }

    /** 将通知字段截断到数据库允许的最大长度。 */
    private String trimToLimit(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }

    /** 返回非空白文本，否则使用默认值。 */
    private String defaultIfBlank(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    /** 校验通知必填文本字段。 */
    private String requireText(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /** 判断字符串是否包含非空白字符。 */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 执行 userUpdate 流程，围绕 user update 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 userUpdate 流程生成的业务结果。
     */
    private LambdaUpdateWrapper<NotificationDO> userUpdate(Long tenantId) {
        LambdaUpdateWrapper<NotificationDO> wrapper = new LambdaUpdateWrapper<>();
        if (tenantId != null) {
            wrapper.eq(NotificationDO::getTenantId, tenantId);
        }
        return wrapper;
    }
}
