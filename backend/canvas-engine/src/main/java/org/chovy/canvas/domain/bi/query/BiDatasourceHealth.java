package org.chovy.canvas.domain.bi.query;

/**
 * BiDatasourceHealth 承载 domain.bi.query 场景中的不可变数据快照。
 * @param sourceKey sourceKey 字段。
 * @param sourceType sourceType 字段。
 * @param available available 字段。
 * @param message message 字段。
 */
public record BiDatasourceHealth(
        String sourceKey,
        String sourceType,
        boolean available,
        String message
) {
}
