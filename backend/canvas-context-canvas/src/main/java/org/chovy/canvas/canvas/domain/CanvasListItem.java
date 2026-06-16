package org.chovy.canvas.canvas.domain;

/**
 * 画布列表项视图。
 */
public record CanvasListItem(
        Long id,
        Long tenantId,
        String name,
        String description,
        Integer status,
        Long publishedVersionId,
        Long canaryVersionId,
        Integer canaryPercent,
        String createdBy,
        Integer isExample,
        String sourceTemplateKey,
        String projectKey,
        String projectName,
        String folderKey,
        String folderName,
        String createdAt,
        String updatedAt,
        String triggerType,
        String cronExpression,
        Integer editVersion,
        String validStart,
        String validEnd,
        Integer maxTotalExecutions,
        Integer perUserDailyLimit,
        Integer perUserTotalLimit,
        Integer cooldownSeconds,
        Integer controlGroupPercent,
        String controlGroupSalt,
        String conversionEventCode,
        Integer attributionWindowDays) {
}
