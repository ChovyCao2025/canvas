package org.chovy.canvas.domain.bi.query;

/**
 * BiQueryCacheInvalidationCommand 承载 domain.bi.query 场景中的不可变数据快照。
 * @param scope scope 字段。
 * @param sqlHash sqlHash 字段。
 * @param datasetKey datasetKey 字段。
 */
public record BiQueryCacheInvalidationCommand(
        String scope,
        String sqlHash,
        String datasetKey) {
}
