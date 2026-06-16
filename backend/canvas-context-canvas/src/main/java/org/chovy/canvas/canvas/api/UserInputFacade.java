package org.chovy.canvas.canvas.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 定义UserInputFacade对外提供的能力契约。
 */
public interface UserInputFacade {

    /**
     * 处理submit。
     */
    SubmitResult submit(Long responseId, SubmitCommand command);

    /**
     * 承载SubmitCommand的数据快照。
     */
    record SubmitCommand(Map<String, Object> response, String operator) {
        public SubmitCommand {
            response = immutableMap(response);
        }
    }

    /**
     * 承载SubmitResult的数据快照。
     */
    record SubmitResult(Long responseId, String status, boolean duplicate) {
    }

    /**
     * 处理immutableMap。
     */
    private static Map<String, Object> immutableMap(Map<String, Object> value) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(value == null ? Map.of() : value));
    }
}
