package org.chovy.canvas.platform.api;

public record WorkstreamStatusView(
        String workstreamKey,
        String displayName,
        String priority,
        String status,
        String childSpecPath,
        String summary) {
}
