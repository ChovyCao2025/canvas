package org.chovy.canvas.canvas.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 定义CanvasTriggerFacade对外提供的能力契约。
 */
public interface CanvasTriggerFacade {

    /**
     * 处理triggerBehavior。
     */
    BehaviorTriggerResult triggerBehavior(BehaviorTriggerCommand command);

    /**
     * 承载BehaviorTriggerCommand的数据快照。
     */
    record BehaviorTriggerCommand(
            /**
             * 记录画布标识。
             */
            Long canvasId,
            /**
             * 记录用户标识。
             */
            String userId,
            /**
             * 记录eventCode。
             */
            String eventCode,
            /**
             * 记录event标识。
             */
            String eventId,
            /**
             * 记录behaviorData。
             */
            Map<String, Object> behaviorData) {

        public BehaviorTriggerCommand {
            requirePositive(canvasId, "canvasId");
            requireString(userId, "userId");
            requireString(eventCode, "eventCode");
            requireString(eventId, "eventId");
            behaviorData = immutableMap(behaviorData);
        }
    }

    /**
     * 承载BehaviorTriggerResult的数据快照。
     */
    record BehaviorTriggerResult(Map<String, Object> data) {

        public BehaviorTriggerResult {
            data = immutableMap(data);
        }
    }

    /**
     * 校验并返回Positive。
     */
    private static void requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    /**
     * 校验并返回String。
     */
    private static void requireString(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    /**
     * 处理immutableMap。
     */
    private static Map<String, Object> immutableMap(Map<String, Object> value) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(value == null ? Map.of() : value));
    }
}
