package org.chovy.canvas.dto.project;

import jakarta.validation.constraints.NotBlank;

/**
 * ProjectUpdateReq 承载 dto.project 场景中的不可变数据快照。
 * @param projectName projectName 字段。
 * @param description description 字段。
 * @param defaultSettingsJson defaultSettingsJson 字段。
 * @param requireReviewBeforePublish requireReviewBeforePublish 字段。
 * @param quietHoursJson quietHoursJson 字段。
 * @param operator operator 字段。
 */
public record ProjectUpdateReq(
        @NotBlank String projectName,
        String description,
        String defaultSettingsJson,
        Integer requireReviewBeforePublish,
        String quietHoursJson,
        String operator
) {
}
