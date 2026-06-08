package org.chovy.canvas.domain.bi.dashboard;

/**
 * BiDashboardResource 承载 domain.bi.dashboard 场景中的不可变数据快照。
 * @param preset preset 字段。
 * @param status status 字段。
 * @param version version 字段。
 * @param source source 字段。
 */
public record BiDashboardResource(
        BiDashboardPreset preset,
        String status,
        int version,
        String source
) {
}
