package org.chovy.canvas.dto.canvas;

import java.time.LocalDateTime;
import java.util.Map;

public record CanvasExportPackage(
        int packageVersion,
        LocalDateTime exportedAt,
        Map<String, Object> source,
        Map<String, Object> canvas,
        Map<String, Object> graph
) {
}
