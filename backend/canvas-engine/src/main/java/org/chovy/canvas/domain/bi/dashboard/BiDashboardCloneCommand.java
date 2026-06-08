package org.chovy.canvas.domain.bi.dashboard;

/**
 * BiDashboardCloneCommand 承载 domain.bi.dashboard 场景中的不可变数据快照。
 * @param dashboardKey dashboardKey 字段。
 * @param title title 字段。
 * @param description description 字段。
 */
public record BiDashboardCloneCommand(
        String dashboardKey,
        String title,
        String description) {
}
