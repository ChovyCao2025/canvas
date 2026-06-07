package org.chovy.canvas.domain.monitoring;

public record MarketingMonitorAnomalyEventQuery(
        Long ruleId,
        String status,
        Integer limit) {
}
