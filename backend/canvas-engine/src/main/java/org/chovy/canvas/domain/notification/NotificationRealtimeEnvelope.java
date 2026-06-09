package org.chovy.canvas.domain.notification;

import org.chovy.canvas.dto.notification.NotificationRealtimePayload;

/**
 * 通知实时推送 Envelope 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 * @param originId 发布通知的服务实例标识，用于避免本实例重复消费.
 * @param tenantId 所属租户 ID，null 表示旧版全局通道.
 * @param userId 实时通知目标用户 ID.
 * @param payload WebSocket 推送的实时通知载荷.
 */
public record NotificationRealtimeEnvelope(
        String originId,
        Long tenantId,
        String userId,
        NotificationRealtimePayload payload
) {
}
