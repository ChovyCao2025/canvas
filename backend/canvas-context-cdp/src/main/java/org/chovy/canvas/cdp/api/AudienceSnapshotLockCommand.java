package org.chovy.canvas.cdp.api;

public record AudienceSnapshotLockCommand(
        Long audienceId,
        Long canvasId,
        Long canvasVersionId,
        String nodeId,
        String operator) {
}
