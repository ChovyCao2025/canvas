package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorAnomalyEventQuery 承载 domain.monitoring 场景中的不可变数据快照。
 * @param ruleId ruleId 字段。
 * @param status status 字段。
 * @param limit limit 字段。
 */
public record MarketingMonitorAnomalyEventQuery(
        Long ruleId,
        String status,
        Integer limit) {
}
