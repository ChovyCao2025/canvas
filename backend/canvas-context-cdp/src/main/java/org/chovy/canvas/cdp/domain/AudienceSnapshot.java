package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.List;

public record AudienceSnapshot(
        Long id,
        Long audienceId,
        Long canvasId,
        Long canvasVersionId,
        String nodeId,
        AudienceSnapshotMode snapshotMode,
        List<String> userIds,
        String createdBy,
        LocalDateTime createdAt) {

    public AudienceSnapshot withId(Long newId) {
        return new AudienceSnapshot(newId, audienceId, canvasId, canvasVersionId, nodeId, snapshotMode, userIds,
                createdBy, createdAt);
    }

    public long userCount() {
        return userIds == null ? 0L : userIds.size();
    }
}
