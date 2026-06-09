package org.chovy.canvas.dto.canvas;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * CanvasExportPackage 承载 dto.canvas 场景中的不可变数据快照。
 * @param packageVersion packageVersion 字段。
 * @param exportedAt exportedAt 字段。
 * @param source source 字段。
 * @param canvas canvas 字段。
 * @param graph graph 字段。
 */
public record CanvasExportPackage(
        int packageVersion,
        LocalDateTime exportedAt,
        Map<String, Object> source,
        Map<String, Object> canvas,
        Map<String, Object> graph
) {
}
