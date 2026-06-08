package org.chovy.canvas.domain.bi.dashboard;

import java.time.LocalDateTime;

/**
 * BiDashboardExportPackage 承载 domain.bi.dashboard 场景中的不可变数据快照。
 * @param resourceType resourceType 字段。
 * @param schemaVersion schemaVersion 字段。
 * @param sourceDashboardKey sourceDashboardKey 字段。
 * @param sourceVersion sourceVersion 字段。
 * @param preset preset 字段。
 * @param exportedBy exportedBy 字段。
 * @param exportedAt exportedAt 字段。
 */
public record BiDashboardExportPackage(
        String resourceType,
        Integer schemaVersion,
        String sourceDashboardKey,
        Integer sourceVersion,
        BiDashboardPreset preset,
        String exportedBy,
        LocalDateTime exportedAt) {
}
