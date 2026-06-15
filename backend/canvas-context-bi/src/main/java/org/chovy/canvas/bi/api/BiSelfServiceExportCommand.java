package org.chovy.canvas.bi.api;

import java.util.Map;

public record BiSelfServiceExportCommand(
        String resourceType,
        String resourceKey,
        Long resourceId,
        String exportFormat,
        Map<String, Object> query,
        Integer rowLimit,
        Boolean approvalRequired,
        Boolean sensitive,
        String approvalReason) {

    public BiSelfServiceExportCommand {
        query = query == null ? Map.of() : Map.copyOf(query);
    }
}
