package org.chovy.canvas.domain.bi.chart;

import java.time.LocalDateTime;

public record BiChartVersionView(
        Long id,
        String chartKey,
        Integer version,
        String status,
        BiChartResource resource,
        String publishedBy,
        LocalDateTime createdAt) {
}
