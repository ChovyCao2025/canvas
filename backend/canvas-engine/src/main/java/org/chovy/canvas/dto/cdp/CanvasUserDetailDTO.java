package org.chovy.canvas.dto.cdp;

import java.util.List;

/**
 * 画布用户 Detail 数据传输对象。
 *
 * <p>用于在控制器、服务、异步任务或实时推送之间传递结构化数据，隔离外部 API 契约与数据库实体。
 * <p>该类型应保持轻量，只表达字段语义和序列化边界，不放入复杂业务流程。
 * @param userId CDP 内部统一用户 ID.
 * @param profile 用户基础画像信息.
 * @param tags 用户当前生效标签列表.
 * @param canvasRows 用户参与过的画布汇总列表.
 */
public record CanvasUserDetailDTO(
        String userId,
        CdpUserDetailDTO profile,
        List<CdpUserTagDTO> tags,
        List<CdpUserCanvasSummaryDTO> canvasRows
) {}
