package org.chovy.canvas.domain.bi.export;

import org.chovy.canvas.domain.bi.query.BiQueryRequest;

public record BiExportJobCommand(
        String resourceType,
        String resourceKey,
        Long resourceId,
        String exportFormat,
        BiQueryRequest query,
        Integer rowLimit,
        Boolean approvalRequired,
        Boolean sensitive,
        String approvalReason) {
}
