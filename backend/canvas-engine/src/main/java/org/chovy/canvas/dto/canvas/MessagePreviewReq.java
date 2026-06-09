package org.chovy.canvas.dto.canvas;

import java.util.Map;

/**
 * MessagePreviewReq 承载 dto.canvas 场景中的不可变数据快照。
 * @param canvasId canvasId 字段。
 * @param nodeId nodeId 字段。
 * @param userId userId 字段。
 * @param graphJson graphJson 字段。
 * @param context context 字段。
 */
public record MessagePreviewReq(
        Long canvasId,
        String nodeId,
        String userId,
        String graphJson,
        Map<String, Object> context
) {
}
