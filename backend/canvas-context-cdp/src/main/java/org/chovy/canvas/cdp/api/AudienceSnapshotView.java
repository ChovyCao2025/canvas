package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;

public record AudienceSnapshotView(
        Long id,
        Long audienceId,
        Long canvasId,
        Long canvasVersionId,
        String nodeId,
        String snapshotMode,
        long userCount,
        String createdBy,
        LocalDateTime createdAt) {
}
