package org.chovy.canvas.dto.cdp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Cdp Batch Tag 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 * @param operationType 批量操作类型，如 BATCH_SET 或 BATCH_REMOVE.
 * @param tagCode 被操作的标签编码，对应 tag_definition.tag_code.
 * @param tagValue 本次批量写入的标签值，删除操作可为空.
 * @param userIds 本次操作涉及的 CDP 用户 ID 列表.
 * @param reason 操作原因或备注，会写入标签变更历史.
 * @param operator 操作人标识.
 */
public record CdpBatchTagReq(
        @NotBlank
        @Size(max = 32)
        String operationType,
        @NotBlank
        @Size(max = 128)
        String tagCode,
        @Size(max = 512)
        String tagValue,
        @NotEmpty
        @Size(max = 10_000)
        List<@NotBlank @Size(max = 128) String> userIds,
        @Size(max = 512)
        String reason,
        @Size(max = 128)
        String operator
) {}
