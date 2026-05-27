package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

/**
 * CDP 用户标签历史 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
public record CdpUserTagHistoryDTO(
        /** 标签编码，对应 tag_definition.tag_code。 */
        String tagCode,
        /** 变更前标签值，新增时为空。 */
        String oldValue,
        /** 变更后标签值，删除时为空。 */
        String newValue,
        /** 变更操作类型，如 SET、REMOVE。 */
        String operation,
        /** 变更来源类型，如 MANUAL、CANVAS、BATCH 或导入来源。 */
        String sourceType,
        /** 来源引用 ID，如执行 ID、导入批次 ID 或外部请求 ID。 */
        String sourceRefId,
        /** 变更原因或操作备注。 */
        String reason,
        /** 操作人标识。 */
        String operator,
        /** 实际操作时间。 */
        LocalDateTime operatedAt
) {}
