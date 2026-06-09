package org.chovy.canvas.domain.monitoring;

/**
 * MarketingMonitorAlertQuery 承载 domain.monitoring 场景中的不可变数据快照。
 * @param status status 字段。
 * @param limit limit 字段。
 */
public record MarketingMonitorAlertQuery(String status,
                                         int limit) {
}
