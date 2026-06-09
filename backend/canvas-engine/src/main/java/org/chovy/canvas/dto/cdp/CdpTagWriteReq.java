package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

/**
 * Cdp Tag Write 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 * @param tagCode 标签编码，对应 tag_definition.tag_code.
 * @param tagValue 标签值，会按标签定义的 valueType 做校验和规范化.
 * @param reason 写入原因或业务备注，会记录到标签历史.
 * @param expiresAt 标签过期时间，null 时可使用标签定义的默认 TTL.
 * @param sourceType 标签来源类型，如 MANUAL、CANVAS、BATCH、API_PUSH、API_PULL 或 EXCEL_IMPORT.
 * @param sourceRefId 来源引用 ID，如画布执行 ID、导入批次 ID 或操作记录 ID.
 * @param operator 操作人标识.
 * @param idempotencyKey 幂等键，用于防止同一标签写入请求重复生效.
 */
public record CdpTagWriteReq(
        String tagCode,
        String tagValue,
        String reason,
        LocalDateTime expiresAt,
        String sourceType,
        String sourceRefId,
        String operator,
        String idempotencyKey
) {}
