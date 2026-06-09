package org.chovy.canvas.dto.notification;

import org.chovy.canvas.dal.dataobject.NotificationDO;

import java.time.LocalDateTime;

/**
 * 通知消息 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 * @param notificationId 对外通知 ID，用于前端、WebSocket 和接口查询.
 * @param type 通知类型，如 TASK、SYSTEM、CANVAS.
 * @param category 通知分类，用于消息中心分组展示.
 * @param severity 通知严重级别，如 INFO、WARN、ERROR.
 * @param status 通知状态，如 UNREAD、READ、ARCHIVED.
 * @param title 通知标题.
 * @param content 通知正文内容.
 * @param targetUrl 点击通知时跳转的目标页面地址.
 * @param actionLabel 操作按钮展示文案.
 * @param actionUrl 操作按钮跳转地址.
 * @param taskId 关联异步任务 ID，非任务通知为空.
 * @param bizType 关联业务类型，如 AUDIENCE、CANVAS、TAG_IMPORT.
 * @param bizId 关联业务对象 ID.
 * @param dedupKey 通知去重键，用于防止同一业务事件重复生成通知.
 * @param payloadJson 通知扩展载荷 JSON.
 * @param readAt 通知已读时间，未读时为空.
 * @param archivedAt 通知归档时间，未归档时为空.
 * @param deliveredAt 通知推送到实时通道的时间，未推送时为空.
 * @param createdAt 通知创建时间.
 */
public record NotificationDTO(
        String notificationId,
        String type,
        String category,
        String severity,
        String status,
        String title,
        String content,
        String targetUrl,
        String actionLabel,
        String actionUrl,
        String taskId,
        String bizType,
        String bizId,
        String dedupKey,
        String payloadJson,
        LocalDateTime readAt,
        LocalDateTime archivedAt,
        LocalDateTime deliveredAt,
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
        // 汇总前面计算出的状态和明细，返回给调用方。
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
