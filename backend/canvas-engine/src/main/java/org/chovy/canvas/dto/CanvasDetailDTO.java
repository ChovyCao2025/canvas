package org.chovy.canvas.dto;

import org.chovy.canvas.domain.canvas.Canvas;
import lombok.Data;

/**
 * 画布详情响应对象。
 *
 * <p>编辑器打开画布时使用，包含“基础信息 + 当前草稿图”。
 * 前端据此初始化画布名称、状态栏与节点图。
 */
@Data
public class CanvasDetailDTO {

    /**
     * 画布基础信息（来自 canvas 主表）。
     *
     * <p>包含名称、描述、状态、配额等“业务外壳”字段。
     */
    private Canvas canvas;

    /**
     * 当前草稿版本的 graph JSON（来自 canvas_version.graph_json）。
     *
     * <p>前端编辑器会据此重建 ReactFlow 的节点和连线。
     */
    private String graphJson;

    /**
     * 当前草稿版本 ID（无草稿时为 null）。
     *
     * <p>用于保存时做版本一致性校验（避免多人并发覆盖）。
     */
    private Long draftVersionId;
}
