package org.chovy.canvas.domain.bi.permission;

import java.time.LocalDateTime;

/**
 * BiColumnPermissionView 承载 domain.bi.permission 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param datasetKey datasetKey 字段。
 * @param datasetId datasetId 字段。
 * @param fieldKey fieldKey 字段。
 * @param subjectType subjectType 字段。
 * @param subjectId subjectId 字段。
 * @param policy policy 字段。
 * @param maskJson maskJson 字段。
 * @param enabled enabled 字段。
 * @param createdAt createdAt 字段。
 */
public record BiColumnPermissionView(
        Long id,
        Long tenantId,
        String datasetKey,
        Long datasetId,
        String fieldKey,
        String subjectType,
        String subjectId,
        String policy,
        String maskJson,
        boolean enabled,
        LocalDateTime createdAt) {
}
