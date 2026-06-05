package org.chovy.canvas.dto.canvas;

import java.util.Map;

public record MessagePreviewReq(
        Long canvasId,
        String nodeId,
        String userId,
        String graphJson,
        Map<String, Object> context
) {
}
