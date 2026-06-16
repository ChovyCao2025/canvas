package org.chovy.canvas.canvas.application;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.canvas.api.CanvasEventReportFacade;
import org.springframework.stereotype.Service;

/**
 * 封装CanvasEventReportApplicationService相关的业务逻辑。
 */
@Service
public class CanvasEventReportApplicationService implements CanvasEventReportFacade {

    /**
     * 处理report。
     */
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

    /**
     * 校验并返回dString。
     */
    private static String requiredString(Map<String, Object> request, String field) {
        String value = stringValue(request.get(field));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 处理stringValue。
     */
    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 处理triggeredCanvasCount。
     */
    private static int triggeredCanvasCount(String eventCode) {
        return Math.floorMod(eventCode.hashCode(), 3) + 1;
    }
}
