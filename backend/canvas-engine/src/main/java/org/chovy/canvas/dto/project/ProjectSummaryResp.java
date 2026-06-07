package org.chovy.canvas.dto.project;

public record ProjectSummaryResp(
        Long id,
        Long tenantId,
        String projectKey,
        String projectName,
        String description,
        String status,
        Integer memberCount,
        Long canvasCount
) {
}
