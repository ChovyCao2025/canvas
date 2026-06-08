package org.chovy.canvas.domain.bi.embed;

/**
 * BiEmbedTokenCleanupResult 承载 domain.bi.embed 场景中的不可变数据快照。
 * @param checked checked 字段。
 * @param revoked revoked 字段。
 * @param failed failed 字段。
 */
public record BiEmbedTokenCleanupResult(
        int checked,
        int revoked,
        int failed
) {
}
