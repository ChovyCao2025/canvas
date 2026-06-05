package org.chovy.canvas.domain.bi.permission;

import java.util.Map;

public record BiColumnPermissionCommand(
        String datasetKey,
        String fieldKey,
        String subjectType,
        String subjectId,
        String policy,
        Map<String, Object> mask,
        Boolean enabled) {
    public BiColumnPermissionCommand {
        mask = mask == null ? Map.of() : Map.copyOf(mask);
    }
}
