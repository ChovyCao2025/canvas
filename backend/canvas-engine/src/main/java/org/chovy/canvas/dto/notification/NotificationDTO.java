package org.chovy.canvas.dto.notification;

import org.chovy.canvas.dal.dataobject.NotificationDO;

import java.time.LocalDateTime;

/**
 * 通知消息 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
public record NotificationDTO(
        /** 对外通知 ID，用于前端、WebSocket 和接口查询。 */
        String notificationId,
        /** 通知类型，如 TASK、SYSTEM、CANVAS。 */
        String type,
        /** 通知分类，用于消息中心分组展示。 */
        String category,
        /** 通知严重级别，如 INFO、WARN、ERROR。 */
        String severity,
        /** 通知状态，如 UNREAD、READ、ARCHIVED。 */
        String status,
        /** 通知标题。 */
        String title,
        /** 通知正文内容。 */
        String content,
        /** 点击通知时跳转的目标页面地址。 */
        String targetUrl,
        /** 操作按钮展示文案。 */
        String actionLabel,
        /** 操作按钮跳转地址。 */
        String actionUrl,
        /** 关联异步任务 ID，非任务通知为空。 */
        String taskId,
        /** 关联业务类型，如 AUDIENCE、CANVAS、TAG_IMPORT。 */
        String bizType,
        /** 关联业务对象 ID。 */
        String bizId,
        /** 通知去重键，用于防止同一业务事件重复生成通知。 */
        String dedupKey,
        /** 通知扩展载荷 JSON。 */
        String payloadJson,
        /** 通知已读时间，未读时为空。 */
        LocalDateTime readAt,
        /** 通知归档时间，未归档时为空。 */
        LocalDateTime archivedAt,
        /** 通知推送到实时通道的时间，未推送时为空。 */
        LocalDateTime deliveredAt,
        /** 通知创建时间。 */
        LocalDateTime createdAt
) {
    /**
     * 执行 from 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param notification notification 方法执行所需的业务参数
     * @return 当前对象实例，便于继续链式配置或后续处理
     */
    public static NotificationDTO from(NotificationDO notification) {
        return new NotificationDTO(
                notification.getNotificationId(),
                notification.getType(),
                notification.getCategory(),
                notification.getSeverity(),
                notification.getStatus(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetUrl(),
                notification.getActionLabel(),
                notification.getActionUrl(),
                notification.getTaskId(),
                notification.getBizType(),
                notification.getBizId(),
                notification.getDedupKey(),
                notification.getPayloadJson(),
                notification.getReadAt(),
                notification.getArchivedAt(),
                notification.getDeliveredAt(),
                notification.getCreatedAt()
        );
    }
}
