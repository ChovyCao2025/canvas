package org.chovy.canvas.domain.notification;

import lombok.Builder;

/**
 * 通知消息 Create Command 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
@Builder
public record NotificationCreateCommand(
        /** 所属租户 ID。 */
        Long tenantId,
        /** 通知接收用户 ID。 */
        String userId,
        /** 通知分类，如 TASK、APPROVAL 或 SYSTEM。 */
        String category,
        /** 通知严重级别，如 INFO、SUCCESS、WARNING 或 ERROR。 */
        String severity,
        /** 通知类型编码，用于区分具体业务事件。 */
        String type,
        /** 通知标题。 */
        String title,
        /** 通知正文内容。 */
        String content,
        /** 通知主跳转地址。 */
        String targetUrl,
        /** 通知操作按钮文案。 */
        String actionLabel,
        /** 通知操作按钮跳转地址。 */
        String actionUrl,
        /** 关联的异步任务业务 ID。 */
        String taskId,
        /** 关联业务类型。 */
        String bizType,
        /** 关联业务对象 ID。 */
        String bizId,
        /** 通知去重键，命中时复用已有通知。 */
        String dedupKey,
        /** 通知扩展载荷 JSON。 */
        String payloadJson
) {
}
