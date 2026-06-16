package org.chovy.canvas.bi.api;
/**
 * BiQueryCacheInvalidationCommand 命令。
 */
public record BiQueryCacheInvalidationCommand(
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * 仪表盘键。
         */
        String dashboardKey,
        String sqlHash) {
}
