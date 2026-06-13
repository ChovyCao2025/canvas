package org.chovy.canvas.platform.domain;

public record PlatformWorkstream(
        String workstreamKey,
        String displayName,
        String priority,
        boolean requiresChildSpec,
        String childSpecPath,
        String summary) {
}
