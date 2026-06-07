package org.chovy.canvas.dto.project;

public record ProjectDetailResp(
        Long id,
        Long tenantId,
        String projectKey,
        String projectName,
        String description,
        String status,
        String defaultSettingsJson,
        Integer requireReviewBeforePublish,
        String quietHoursJson
) {
}
