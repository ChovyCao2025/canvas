package org.chovy.canvas.dto.cdp;

import java.util.List;

/**
 * Cdp Batch Tag 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
public record CdpBatchTagReq(
        /** 批量操作类型，如 BATCH_SET 或 BATCH_REMOVE。 */
        String operationType,
        /** 被操作的标签编码，对应 tag_definition.tag_code。 */
        String tagCode,
        /** 本次批量写入的标签值，删除操作可为空。 */
        String tagValue,
        /** 本次操作涉及的 CDP 用户 ID 列表。 */
        List<String> userIds,
        /** 操作原因或备注，会写入标签变更历史。 */
        String reason,
        /** 操作人标识。 */
        String operator
) {}
