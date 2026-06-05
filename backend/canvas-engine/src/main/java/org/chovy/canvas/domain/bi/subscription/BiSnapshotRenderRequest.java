package org.chovy.canvas.domain.bi.subscription;

import java.util.Map;

public record BiSnapshotRenderRequest(
        String html,
        String resourceUrl,
        String format,
        int width,
        int height,
        double scale,
        Map<String, Object> metadata
) {
    public BiSnapshotRenderRequest {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
