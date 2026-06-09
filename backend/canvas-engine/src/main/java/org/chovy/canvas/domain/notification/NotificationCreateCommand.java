package org.chovy.canvas.domain.notification;

import lombok.Builder;

/**
 * 通知消息 Create Command 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
@Builder
/**
 * NotificationCreateCommand record.
 * @param tenantId 所属租户 ID.
 * @param userId 通知接收用户 ID.
 * @param category 通知分类，如 TASK、APPROVAL 或 SYSTEM.
 * @param severity 通知严重级别，如 INFO、SUCCESS、WARNING 或 ERROR.
 * @param type 通知类型编码，用于区分具体业务事件.
 * @param title 通知标题.
 * @param content 通知正文内容.
 * @param targetUrl 通知主跳转地址.
 * @param actionLabel 通知操作按钮文案.
 * @param actionUrl 通知操作按钮跳转地址.
 * @param taskId 关联的异步任务业务 ID.
 * @param bizType 关联业务类型.
 * @param bizId 关联业务对象 ID.
 * @param dedupKey 通知去重键，命中时复用已有通知.
 * @param payloadJson 通知扩展载荷 JSON.
 */
public record NotificationCreateCommand(
        Long tenantId,
        String userId,
        String category,
        String severity,
        String type,
        String title,
        String content,
        String targetUrl,
        String actionLabel,
        String actionUrl,
        String taskId,
        String bizType,
        String bizId,
        String dedupKey,
        String payloadJson
) {
}
