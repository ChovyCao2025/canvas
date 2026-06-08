package org.chovy.canvas.domain.bi.permission;

import java.time.LocalDateTime;

/**
 * BiRowPermissionView 承载 domain.bi.permission 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param datasetKey datasetKey 字段。
 * @param datasetId datasetId 字段。
 * @param ruleKey ruleKey 字段。
 * @param subjectType subjectType 字段。
 * @param subjectId subjectId 字段。
 * @param filterJson filterJson 字段。
 * @param enabled enabled 字段。
 * @param createdAt createdAt 字段。
 */
public record BiRowPermissionView(
        Long id,
        Long tenantId,
        String datasetKey,
        Long datasetId,
        String ruleKey,
        String subjectType,
        String subjectId,
        String filterJson,
        boolean enabled,
        LocalDateTime createdAt) {
}
