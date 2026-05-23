package org.chovy.canvas.perf;

import java.util.Map;
import java.util.regex.Pattern;

public final class PerfRunContext {

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_:-]{1,80}");

    private PerfRunContext() {
    }

    public static String extract(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        Object raw = payload.get("perfRunId");
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isBlank() || !SAFE_ID.matcher(value).matches()) {
            return null;
        }
        return value;
    }
}
