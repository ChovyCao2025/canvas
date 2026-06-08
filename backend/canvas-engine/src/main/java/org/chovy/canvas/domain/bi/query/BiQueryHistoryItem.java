package org.chovy.canvas.domain.bi.query;

import java.time.LocalDateTime;

/**
 * BiQueryHistoryItem 承载 domain.bi.query 场景中的不可变数据快照。
 * @param id id 字段。
 * @param datasetKey datasetKey 字段。
 * @param username username 字段。
 * @param rowCount rowCount 字段。
 * @param durationMs durationMs 字段。
 * @param status status 字段。
 * @param sqlHash sqlHash 字段。
 * @param errorMessage errorMessage 字段。
 * @param createdAt createdAt 字段。
 */
public record BiQueryHistoryItem(
        Long id,
        String datasetKey,
        String username,
        int rowCount,
        long durationMs,
        String status,
        String sqlHash,
        String errorMessage,
        LocalDateTime createdAt
) {
}
