package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

/**
 * CDP 用户标签 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 * @param tagCode 标签编码，对应 tag_definition.tag_code.
 * @param tagName 标签展示名称，缺省时可能与 tagCode 相同.
 * @param tagValue 当前生效的标签值.
 * @param valueType 标签值类型，如 STRING、NUMBER、BOOLEAN、JSON.
 * @param sourceType 标签来源类型，如 MANUAL、CANVAS、BATCH、API_PUSH、API_PULL 或 EXCEL_IMPORT.
 * @param status 标签状态，如 ACTIVE、EXPIRED、REMOVED.
 * @param effectiveAt 标签生效时间.
 * @param expiresAt 标签过期时间，null 表示长期有效.
 * @param updatedAt 标签最后更新时间.
 */
public record CdpUserTagDTO(
        String tagCode,
        String tagName,
        String tagValue,
        String valueType,
        String sourceType,
        String status,
        LocalDateTime effectiveAt,
        LocalDateTime expiresAt,
        LocalDateTime updatedAt
) {}
