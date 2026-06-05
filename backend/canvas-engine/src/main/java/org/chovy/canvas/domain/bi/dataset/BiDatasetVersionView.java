package org.chovy.canvas.domain.bi.dataset;

import java.time.LocalDateTime;

public record BiDatasetVersionView(
        Long id,
        String datasetKey,
        Integer version,
        String status,
        BiDatasetResource resource,
        String publishedBy,
        LocalDateTime createdAt) {
}
