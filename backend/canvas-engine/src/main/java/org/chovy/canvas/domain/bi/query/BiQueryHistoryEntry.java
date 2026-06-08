package org.chovy.canvas.domain.bi.query;

/**
 * BiQueryHistoryEntry 承载 domain.bi.query 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param username username 字段。
 * @param request request 字段。
 * @param sqlHash sqlHash 字段。
 * @param rowCount rowCount 字段。
 * @param durationMs durationMs 字段。
 * @param status status 字段。
 * @param errorMessage errorMessage 字段。
 */
public record BiQueryHistoryEntry(
        Long tenantId,
        String username,
        BiQueryRequest request,
        String sqlHash,
        int rowCount,
        long durationMs,
        String status,
        String errorMessage
) {
}
