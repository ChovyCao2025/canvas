package org.chovy.canvas.dto.project;

import jakarta.validation.constraints.NotBlank;

public record ProjectCreateReq(
        @NotBlank String projectKey,
        @NotBlank String projectName,
        String description,
        String defaultSettingsJson,
        Integer requireReviewBeforePublish,
        String quietHoursJson,
        String operator
) {
}
