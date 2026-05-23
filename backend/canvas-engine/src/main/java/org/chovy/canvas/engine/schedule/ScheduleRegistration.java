package org.chovy.canvas.engine.schedule;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Scheduler-agnostic registration payload.
 */
public record ScheduleRegistration(
        ScheduleKey key,
        String cronExpression,
        LocalDateTime triggerTime,
        String timezone,
        Runnable callback,
        Map<String, Object> metadata
) {
    public ScheduleRegistration {
        if (key == null) {
            throw new IllegalArgumentException("Schedule key must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("Schedule callback must not be null");
        }
        boolean hasCron = cronExpression != null && !cronExpression.isBlank();
        boolean hasTriggerTime = triggerTime != null;
        if (hasCron == hasTriggerTime) {
            throw new IllegalArgumentException("Exactly one of cronExpression or triggerTime must be set");
        }
        if (timezone == null || timezone.isBlank()) {
            timezone = "Asia/Shanghai";
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
