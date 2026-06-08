package org.chovy.canvas.domain.bi.query;

/**
 * BiQueryColumn 承载 domain.bi.query 场景中的不可变数据快照。
 * @param key key 字段。
 * @param role role 字段。
 * @param dataType dataType 字段。
 */
public record BiQueryColumn(
        String key,
        String role,
        String dataType
) {
}
