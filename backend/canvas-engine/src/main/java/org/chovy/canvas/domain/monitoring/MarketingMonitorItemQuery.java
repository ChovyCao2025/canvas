package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorItemQuery 承载 domain.monitoring 场景中的不可变数据快照。
 * @param sentimentLabel sentimentLabel 字段。
 * @param competitorKey competitorKey 字段。
 * @param limit limit 字段。
 */
public record MarketingMonitorItemQuery(String sentimentLabel,
                                        String competitorKey,
                                        int limit) {
}
