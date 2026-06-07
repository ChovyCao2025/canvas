package org.chovy.canvas.domain.bi.dataset;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;

public record BiDatasetDraftNormalization(
        BiDatasetResource resource,
        BiDatasetSpec spec
) {
}
