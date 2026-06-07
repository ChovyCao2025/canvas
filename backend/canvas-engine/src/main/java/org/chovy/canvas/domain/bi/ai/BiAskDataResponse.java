package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;

public record BiAskDataResponse(
        String question,
        String status,
        boolean fallbackUsed,
        String explanation,
        BiQueryRequest query,
        BiQueryResult result
) {
}
