package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 画布用户 Row 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
public record CanvasUserRowDTO(
        /** CDP 内部统一用户 ID。 */
        String userId,
        /** 前端列表展示名称，缺省时使用 userId。 */
        String displayName,
        /** 用户进入或执行画布的总次数。 */
        long executionCount,
        /** 执行成功次数。 */
        long successCount,
        /** 执行失败次数。 */
        long failedCount,
        /** 最近一次执行状态展示值。 */
        String latestStatus,
        /** 用户首次进入该统计范围的时间。 */
        LocalDateTime firstEnteredAt,
        /** 用户最近一次进入该统计范围的时间。 */
        LocalDateTime lastEnteredAt,
        /** 用户当前生效标签列表。 */
        List<CdpUserTagDTO> tags
) {}
