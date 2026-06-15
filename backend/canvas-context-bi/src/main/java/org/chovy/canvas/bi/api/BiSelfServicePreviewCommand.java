package org.chovy.canvas.bi.api;

import java.util.Map;

public record BiSelfServicePreviewCommand(
        Map<String, Object> query,
        Integer previewLimit) {

    public BiSelfServicePreviewCommand {
        query = query == null ? Map.of() : Map.copyOf(query);
    }
}
