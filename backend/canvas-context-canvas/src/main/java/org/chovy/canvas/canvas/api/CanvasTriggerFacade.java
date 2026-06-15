package org.chovy.canvas.canvas.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public interface CanvasTriggerFacade {

    BehaviorTriggerResult triggerBehavior(BehaviorTriggerCommand command);

    record BehaviorTriggerCommand(
            Long canvasId,
            String userId,
            String eventCode,
            String eventId,
            Map<String, Object> behaviorData) {

        public BehaviorTriggerCommand {
            requirePositive(canvasId, "canvasId");
            requireString(userId, "userId");
            requireString(eventCode, "eventCode");
            requireString(eventId, "eventId");
            behaviorData = immutableMap(behaviorData);
        }
    }

    record BehaviorTriggerResult(Map<String, Object> data) {

        public BehaviorTriggerResult {
            data = immutableMap(data);
        }
    }

    private static void requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static void requireString(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static Map<String, Object> immutableMap(Map<String, Object> value) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(value == null ? Map.of() : value));
    }
}
