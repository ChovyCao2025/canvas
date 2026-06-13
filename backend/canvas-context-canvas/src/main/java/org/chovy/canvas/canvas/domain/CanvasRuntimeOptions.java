package org.chovy.canvas.canvas.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public record CanvasRuntimeOptions(
        String triggerType,
        String cronExpression,
        String validStart,
        String validEnd,
        Integer maxTotalExecutions,
        Integer perUserDailyLimit,
        Integer perUserTotalLimit,
        Integer cooldownSeconds,
        Integer controlGroupPercent,
        String controlGroupSalt,
        String conversionEventCode,
        Integer attributionWindowDays,
        String attributionModel) {

    public static CanvasRuntimeOptions empty() {
        return new CanvasRuntimeOptions(null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    public Map<String, Object> toExecutionOptions() {
        Map<String, Object> options = new LinkedHashMap<>();
        putIfPresent(options, "triggerType", triggerType);
        putIfPresent(options, "cronExpression", cronExpression);
        putIfPresent(options, "validStart", validStart);
        putIfPresent(options, "validEnd", validEnd);
        putIfPresent(options, "maxTotalExecutions", maxTotalExecutions);
        putIfPresent(options, "perUserDailyLimit", perUserDailyLimit);
        putIfPresent(options, "perUserTotalLimit", perUserTotalLimit);
        putIfPresent(options, "cooldownSeconds", cooldownSeconds);
        putIfPresent(options, "controlGroupPercent", controlGroupPercent);
        putIfPresent(options, "controlGroupSalt", controlGroupSalt);
        putIfPresent(options, "conversionEventCode", conversionEventCode);
        putIfPresent(options, "attributionWindowDays", attributionWindowDays);
        putIfPresent(options, "attributionModel", attributionModel);
        return Map.copyOf(options);
    }

    private static void putIfPresent(Map<String, Object> options, String key, Object value) {
        if (value != null) {
            options.put(key, value);
        }
    }
}
