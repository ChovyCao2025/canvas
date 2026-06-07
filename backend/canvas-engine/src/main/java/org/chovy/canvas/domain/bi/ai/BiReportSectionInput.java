package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;

public record BiReportSectionInput(
        String title,
        BiQueryRequest query,
        BiQueryResult result
) {
}
