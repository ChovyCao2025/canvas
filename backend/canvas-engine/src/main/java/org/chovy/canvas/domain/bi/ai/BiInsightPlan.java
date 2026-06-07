package org.chovy.canvas.domain.bi.ai;

import java.util.List;

public record BiInsightPlan(
        List<String> trends,
        List<String> anomalies,
        List<String> opportunities
) {
    public BiInsightPlan {
        trends = trends == null ? List.of() : List.copyOf(trends);
        anomalies = anomalies == null ? List.of() : List.copyOf(anomalies);
        opportunities = opportunities == null ? List.of() : List.copyOf(opportunities);
    }
}
