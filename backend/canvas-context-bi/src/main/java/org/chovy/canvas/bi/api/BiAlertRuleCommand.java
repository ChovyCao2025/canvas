package org.chovy.canvas.bi.api;

import java.util.Map;

public record BiAlertRuleCommand(
        String alertKey,
        String name,
        String datasetKey,
        String metricKey,
        Map<String, Object> condition,
        Map<String, Object> receivers,
        Boolean enabled) {

    public BiAlertRuleCommand {
        condition = condition == null ? Map.of() : Map.copyOf(condition);
        receivers = receivers == null ? Map.of() : Map.copyOf(receivers);
    }
}
