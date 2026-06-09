package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

/**
 * CDP 用户 Detail 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 * @param userId CDP 内部统一用户 ID.
 * @param displayName 用户展示名称.
 * @param phone 脱敏后的用户手机号.
 * @param email 脱敏后的用户邮箱.
 * @param status 用户状态，如 ACTIVE、DISABLED.
 * @param propertiesJson 扩展画像属性 JSON.
 * @param firstSeenAt 首次识别到该用户的时间.
 * @param lastSeenAt 最近一次识别或更新该用户的时间.
 */
public record CdpUserDetailDTO(
        String userId,
        String displayName,
        String phone,
        String email,
        String status,
        String propertiesJson,
        LocalDateTime firstSeenAt,
        LocalDateTime lastSeenAt
) {}
