package org.chovy.canvas.domain.bi.dataset;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;

/**
 * BiDatasetDraftNormalization 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param resource resource 字段。
 * @param spec spec 字段。
 */
public record BiDatasetDraftNormalization(
        BiDatasetResource resource,
        BiDatasetSpec spec
) {
}
