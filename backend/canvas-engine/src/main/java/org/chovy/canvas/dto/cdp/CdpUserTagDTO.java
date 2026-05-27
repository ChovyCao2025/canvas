package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

/**
 * CDP 用户标签 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
public record CdpUserTagDTO(
        /** 标签编码，对应 tag_definition.tag_code。 */
        String tagCode,
        /** 标签展示名称，缺省时可能与 tagCode 相同。 */
        String tagName,
        /** 当前生效的标签值。 */
        String tagValue,
        /** 标签值类型，如 STRING、NUMBER、BOOLEAN、JSON。 */
        String valueType,
        /** 标签来源类型，如 MANUAL、CANVAS、BATCH、API_PUSH、API_PULL 或 EXCEL_IMPORT。 */
        String sourceType,
        /** 标签状态，如 ACTIVE、EXPIRED、REMOVED。 */
        String status,
        /** 标签生效时间。 */
        LocalDateTime effectiveAt,
        /** 标签过期时间，null 表示长期有效。 */
        LocalDateTime expiresAt,
        /** 标签最后更新时间。 */
        LocalDateTime updatedAt
) {}
