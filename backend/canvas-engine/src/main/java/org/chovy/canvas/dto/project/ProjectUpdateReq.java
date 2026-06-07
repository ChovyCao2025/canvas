package org.chovy.canvas.dto.project;

import jakarta.validation.constraints.NotBlank;

public record ProjectUpdateReq(
        @NotBlank String projectName,
        String description,
        String defaultSettingsJson,
        Integer requireReviewBeforePublish,
        String quietHoursJson,
        String operator
) {
}
