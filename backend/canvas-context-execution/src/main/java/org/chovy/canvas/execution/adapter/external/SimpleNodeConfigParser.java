package org.chovy.canvas.execution.adapter.external;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.execution.domain.NodeConfigParser;

public class SimpleNodeConfigParser implements NodeConfigParser {

    @Override
    public Map<String, Object> parse(String configJson, String nodeId) {
        if (configJson == null || configJson.isBlank() || "{}".equals(configJson.trim())) {
            return Map.of();
        }
        String trimmed = configJson.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new IllegalArgumentException("node config JSON parse failed: nodeId=" + nodeId);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isBlank()) {
            return Map.of();
        }
        for (String pair : body.split(",")) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length != 2) {
                throw new IllegalArgumentException("node config JSON parse failed: nodeId=" + nodeId);
            }
            result.put(unquote(keyValue[0].trim()), scalar(keyValue[1].trim()));
        }
        return Map.copyOf(result);
    }

    private Object scalar(String raw) {
        if ("true".equals(raw)) {
            return true;
        }
        if ("false".equals(raw)) {
            return false;
        }
        if (raw.matches("-?\\d+")) {
            return Long.parseLong(raw);
        }
        return unquote(raw);
    }

    private String unquote(String raw) {
        if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }
}
