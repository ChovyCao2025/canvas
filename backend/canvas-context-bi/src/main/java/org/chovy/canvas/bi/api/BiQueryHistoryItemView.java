package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
/**
 * BiQueryHistoryItemView 视图。
 */
public record BiQueryHistoryItemView(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * 数据集键。
         */
        String datasetKey,
        /**
         * username 字段值。
         */
        String username,
        /**
         * rowCount 对应的统计数量。
         */
        int rowCount,
        /**
         * durationMs 对应的数据集合。
         */
        long durationMs,
        /**
         * 状态值。
         */
        String status,
        /**
         * sqlHash 字段值。
         */
        String sqlHash,
        /**
         * errorMessage 字段值。
         */
        String errorMessage,
        LocalDateTime createdAt) {
}
