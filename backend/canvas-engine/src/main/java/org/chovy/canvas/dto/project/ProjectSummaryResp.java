package org.chovy.canvas.dto.project;

/**
 * ProjectSummaryResp 承载 dto.project 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param projectKey projectKey 字段。
 * @param projectName projectName 字段。
 * @param description description 字段。
 * @param status status 字段。
 * @param memberCount memberCount 字段。
 * @param canvasCount canvasCount 字段。
 */
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
