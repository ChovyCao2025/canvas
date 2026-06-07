package org.chovy.canvas.domain.providerwrite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ProviderWriteEvidenceSanitizer {

    public static final String REDACTED = "[REDACTED]";

    private static final Set<String> SECRET_KEYS = Set.of(
            "token",
            "accesstoken",
            "refreshtoken",
            "clientsecret",
            "developertoken",
            "apikey",
            "password",
            "secret",
            "authorization",
            "credential",
            "credentials",
            "soapheader",
            "soapheaders");

    private ProviderWriteEvidenceSanitizer() {
    }

    public static Map<String, Object> sanitizeMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        values.forEach((key, value) -> sanitized.put(key, sanitizeValue(key, value)));
        return sanitized;
    }

    @SuppressWarnings("unchecked")
    public static Object sanitize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> sanitized.put(String.valueOf(key), sanitizeValue(key, nestedValue)));
            return sanitized;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> sanitized = new ArrayList<>();
            iterable.forEach(item -> sanitized.add(sanitize(item)));
            return List.copyOf(sanitized);
        }
        return value;
    }

    private static Object sanitizeValue(Object key, Object value) {
        if (isSecretKey(key)) {
            return REDACTED;
        }
        return sanitize(value);
    }

    private static boolean isSecretKey(Object key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toString()
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
        return SECRET_KEYS.contains(normalized);
    }
}
