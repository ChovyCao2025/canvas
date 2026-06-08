package org.chovy.canvas.domain.bi.dashboard;

import java.time.LocalDateTime;

/**
 * BiDashboardVersionView 承载 domain.bi.dashboard 场景中的不可变数据快照。
 * @param id id 字段。
 * @param dashboardKey dashboardKey 字段。
 * @param version version 字段。
 * @param status status 字段。
 * @param preset preset 字段。
 * @param publishedBy publishedBy 字段。
 * @param createdAt createdAt 字段。
 */
public record BiDashboardVersionView(
        Long id,
        String dashboardKey,
        Integer version,
        String status,
        BiDashboardPreset preset,
        String publishedBy,
        LocalDateTime createdAt) {
}
