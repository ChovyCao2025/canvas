package org.chovy.canvas.domain.bi.chart;

import java.time.LocalDateTime;

/**
 * BiChartVersionView 承载 domain.bi.chart 场景中的不可变数据快照。
 * @param id id 字段。
 * @param chartKey chartKey 字段。
 * @param version version 字段。
 * @param status status 字段。
 * @param resource resource 字段。
 * @param publishedBy publishedBy 字段。
 * @param createdAt createdAt 字段。
 */
public record BiChartVersionView(
        Long id,
        String chartKey,
        Integer version,
        String status,
        BiChartResource resource,
        String publishedBy,
        LocalDateTime createdAt) {
}
