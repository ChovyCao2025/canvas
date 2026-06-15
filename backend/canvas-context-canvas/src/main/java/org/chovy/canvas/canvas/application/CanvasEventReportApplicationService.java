package org.chovy.canvas.canvas.application;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasEventReportFacade;
import org.springframework.stereotype.Service;

@Service
public class CanvasEventReportApplicationService implements CanvasEventReportFacade {

    @Override
    public Map<String, Object> report(String rawBody) {
        Map<String, Object> request = JsonSupport.parseObject(rawBody == null || rawBody.isBlank() ? "{}" : rawBody);
        String eventCode = requiredString(request, "eventCode");
        String userId = requiredString(request, "userId");
        String idempotencyKey = stringValue(request.get("idempotencyKey"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventCode", eventCode);
        result.put("userId", userId);
        result.put("accepted", true);
        result.put("triggeredCanvasCount", triggeredCanvasCount(eventCode));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            result.put("idempotencyKey", idempotencyKey);
        }
        return result;
    }

    private static String requiredString(Map<String, Object> request, String field) {
        String value = stringValue(request.get(field));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int triggeredCanvasCount(String eventCode) {
        return Math.floorMod(eventCode.hashCode(), 3) + 1;
    }
}
