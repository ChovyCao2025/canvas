package org.chovy.canvas.domain.bi.export;

import org.chovy.canvas.domain.bi.query.BiQueryRequest;

/**
 * BiSelfServicePreviewRequest 承载 domain.bi.export 场景中的不可变数据快照。
 * @param query query 字段。
 * @param previewLimit previewLimit 字段。
 */
public record BiSelfServicePreviewRequest(
        BiQueryRequest query,
        Integer previewLimit) {
}
