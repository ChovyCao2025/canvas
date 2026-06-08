package org.chovy.canvas.domain.bi.ai;

import java.util.List;

/**
 * BiInsightPlan 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param trends trends 字段。
 * @param anomalies anomalies 字段。
 * @param opportunities opportunities 字段。
 */
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
