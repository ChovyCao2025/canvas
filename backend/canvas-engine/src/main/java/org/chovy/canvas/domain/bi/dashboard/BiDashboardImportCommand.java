package org.chovy.canvas.domain.bi.dashboard;

/**
 * BiDashboardImportCommand 承载 domain.bi.dashboard 场景中的不可变数据快照。
 * @param packagePayload packagePayload 字段。
 * @param dashboardKey dashboardKey 字段。
 * @param title title 字段。
 * @param overwrite overwrite 字段。
 */
public record BiDashboardImportCommand(
        BiDashboardExportPackage packagePayload,
        String dashboardKey,
        String title,
        Boolean overwrite) {
}
