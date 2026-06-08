package org.chovy.canvas.domain.bi.query;

/**
 * BiQueryCacheInvalidationResult 承载 domain.bi.query 场景中的不可变数据快照。
 * @param scope scope 字段。
 * @param deletedEntries deletedEntries 字段。
 * @param message message 字段。
 */
public record BiQueryCacheInvalidationResult(
        String scope,
        int deletedEntries,
        String message) {
}
