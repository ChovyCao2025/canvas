package org.chovy.canvas.dto.canvas;

import org.chovy.canvas.dal.dataobject.CanvasDO;

/**
 * CanvasImportResp 承载 dto.canvas 场景中的不可变数据快照。
 * @param canvas canvas 字段。
 * @param draftVersionId draftVersionId 字段。
 */
public record CanvasImportResp(CanvasDO canvas, Long draftVersionId) {
}
