package org.chovy.canvas.bi.api;
/**
 * BiDashboardImportCommand 命令。
 */
public record BiDashboardImportCommand(
        /**
         * packageView 字段值。
         */
        BiDashboardExportPackageView packageView,
        /**
         * 仪表盘键。
         */
        String dashboardKey,
        /**
         * 展示名称。
         */
        String name,
        /**
         * 是否覆盖已有资源。
         */
        boolean overwrite
) {
}
