package org.chovy.canvas.dto.canvas;

import java.util.List;
import java.util.Map;

public record MessagePreviewResp(
        String channel,
        String templateId,
        Map<String, Object> content,
        Map<String, Object> variables,
        List<String> warnings
) {
}
