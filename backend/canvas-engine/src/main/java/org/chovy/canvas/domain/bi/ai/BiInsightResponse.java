package org.chovy.canvas.domain.bi.ai;

import java.util.List;

public record BiInsightResponse(
        String status,
        boolean fallbackUsed,
        List<String> trends,
        List<String> anomalies,
        List<String> opportunities
) {
    public BiInsightResponse {
        trends = trends == null ? List.of() : List.copyOf(trends);
        anomalies = anomalies == null ? List.of() : List.copyOf(anomalies);
        opportunities = opportunities == null ? List.of() : List.copyOf(opportunities);
    }
}
