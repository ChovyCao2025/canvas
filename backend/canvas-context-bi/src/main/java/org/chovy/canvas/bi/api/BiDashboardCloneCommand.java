package org.chovy.canvas.bi.api;
/**
 * BiDashboardCloneCommand 命令。
 */
public record BiDashboardCloneCommand(
        /**
         * 仪表盘键。
         */
        String dashboardKey,
        /**
         * 展示名称。
         */
        String name,
        /**
         * 说明文本。
         */
        String description
) {
}
