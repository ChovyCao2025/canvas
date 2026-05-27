package org.chovy.canvas.dto.cdp;

import java.time.LocalDateTime;

/**
 * CDP 用户 Canvas Summary 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
public record CdpUserCanvasSummaryDTO(
        /** 画布 ID。 */
        Long canvasId,
        /** 画布名称。 */
        String canvasName,
        /** 用户在该画布下的执行总次数。 */
        long executionCount,
        /** 用户在该画布下的成功执行次数。 */
        long successCount,
        /** 用户在该画布下的失败执行次数。 */
        long failedCount,
        /** 用户在该画布下最近一次执行状态展示值。 */
        String latestStatus,
        /** 用户首次进入该画布的时间。 */
        LocalDateTime firstEnteredAt,
        /** 用户最近一次进入该画布的时间。 */
        LocalDateTime lastEnteredAt
) {}
