package org.chovy.canvas.canvas.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public interface UserInputFacade {

    SubmitResult submit(Long responseId, SubmitCommand command);

    record SubmitCommand(Map<String, Object> response, String operator) {
        public SubmitCommand {
            response = immutableMap(response);
        }
    }

    record SubmitResult(Long responseId, String status, boolean duplicate) {
    }

    private static Map<String, Object> immutableMap(Map<String, Object> value) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(value == null ? Map.of() : value));
    }
}
