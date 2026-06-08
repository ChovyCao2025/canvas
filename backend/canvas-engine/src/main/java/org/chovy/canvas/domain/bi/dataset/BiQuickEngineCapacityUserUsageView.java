package org.chovy.canvas.domain.bi.dataset;

/**
 * BiQuickEngineCapacityUserUsageView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param user user 字段。
 * @param usedRows usedRows 字段。
 * @param activeTables activeTables 字段。
 * @param resourceCount resourceCount 字段。
 */
public record BiQuickEngineCapacityUserUsageView(
        String user,
        long usedRows,
        int activeTables,
        int resourceCount) {
}
