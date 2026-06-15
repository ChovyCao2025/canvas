package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiRowPermissionCommand(
        String datasetKey,
        String ruleKey,
        String subjectType,
        String subjectId,
        List<Map<String, Object>> filters,
        Map<String, Object> filter,
        Boolean enabled) {

    public BiRowPermissionCommand {
        filters = filters == null ? List.of() : List.copyOf(filters);
        filter = filter == null ? Map.of() : Map.copyOf(filter);
    }
}
