package org.chovy.canvas.domain.bi.ai;

import java.util.List;

/**
 * BiInsightResponse 承载 domain.bi.ai 场景中的不可变数据快照。
 * @param status status 字段。
 * @param fallbackUsed fallbackUsed 字段。
 * @param trends trends 字段。
 * @param anomalies anomalies 字段。
 * @param opportunities opportunities 字段。
 */
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
