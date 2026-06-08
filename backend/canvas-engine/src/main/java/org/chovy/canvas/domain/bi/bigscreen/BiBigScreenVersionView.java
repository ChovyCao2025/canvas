package org.chovy.canvas.domain.bi.bigscreen;

import java.time.LocalDateTime;

/**
 * BiBigScreenVersionView 承载 domain.bi.bigscreen 场景中的不可变数据快照。
 * @param id id 字段。
 * @param screenKey screenKey 字段。
 * @param version version 字段。
 * @param status status 字段。
 * @param resource resource 字段。
 * @param publishedBy publishedBy 字段。
 * @param createdAt createdAt 字段。
 */
public record BiBigScreenVersionView(
        Long id,
        String screenKey,
        Integer version,
        String status,
        BiBigScreenResource resource,
        String publishedBy,
        LocalDateTime createdAt) {
}
