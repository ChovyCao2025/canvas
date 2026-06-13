package org.chovy.canvas.risk.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record RiskDecisionCommand(
        Long tenantId,
        String requestId,
        String sceneKey,
        Instant eventTime,
        Map<String, Object> subject,
        Map<String, Object> event,
        Map<String, Object> context,
        Map<String, Object> features,
        int deadlineMs) {

    public RiskDecisionCommand {
        subject = subject == null ? Map.of() : new LinkedHashMap<>(subject);
        event = event == null ? Map.of() : new LinkedHashMap<>(event);
        context = context == null ? Map.of() : new LinkedHashMap<>(context);
        features = features == null ? Map.of() : new LinkedHashMap<>(features);
    }
}
