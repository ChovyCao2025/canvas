package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;

/**
 * BiDatasetVersionView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param id id 字段。
 * @param datasetKey datasetKey 字段。
 * @param version version 字段。
 * @param status status 字段。
 * @param resource resource 字段。
 * @param publishedBy publishedBy 字段。
 * @param createdAt createdAt 字段。
 */
public record BiDatasetVersionView(
        Long id,
        String datasetKey,
        Integer version,
        String status,
        BiDatasetResource resource,
        String publishedBy,
        LocalDateTime createdAt) {
}
