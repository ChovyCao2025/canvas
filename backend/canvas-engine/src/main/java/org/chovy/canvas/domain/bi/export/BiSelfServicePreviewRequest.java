package org.chovy.canvas.domain.bi.export;

import org.chovy.canvas.domain.bi.query.BiQueryRequest;

public record BiSelfServicePreviewRequest(
        BiQueryRequest query,
        Integer previewLimit) {
}
