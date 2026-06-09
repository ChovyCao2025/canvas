package org.chovy.canvas.dto.project;

/**
 * ProjectDetailResp 承载 dto.project 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param projectKey projectKey 字段。
 * @param projectName projectName 字段。
 * @param description description 字段。
 * @param status status 字段。
 * @param defaultSettingsJson defaultSettingsJson 字段。
 * @param requireReviewBeforePublish requireReviewBeforePublish 字段。
 * @param quietHoursJson quietHoursJson 字段。
 */
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
