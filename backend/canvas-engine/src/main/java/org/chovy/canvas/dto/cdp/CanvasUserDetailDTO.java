package org.chovy.canvas.dto.cdp;

import java.util.List;

/**
 * 画布用户 Detail 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 */
public record CanvasUserDetailDTO(
        /** CDP 内部统一用户 ID。 */
        String userId,
        /** 用户基础画像信息。 */
        CdpUserDetailDTO profile,
        /** 用户当前生效标签列表。 */
        List<CdpUserTagDTO> tags,
        /** 用户参与过的画布汇总列表。 */
        List<CdpUserCanvasSummaryDTO> canvasRows
) {}
