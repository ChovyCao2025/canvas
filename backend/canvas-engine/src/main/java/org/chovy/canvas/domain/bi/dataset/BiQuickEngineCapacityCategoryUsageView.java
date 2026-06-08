package org.chovy.canvas.domain.bi.dataset;

/**
 * BiQuickEngineCapacityCategoryUsageView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param type type 字段。
 * @param usedRows usedRows 字段。
 * @param resourceCount resourceCount 字段。
 */
public record BiQuickEngineCapacityCategoryUsageView(
        String type,
        long usedRows,
        int resourceCount) {
}
