package org.chovy.canvas.execution.adapter.external;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.execution.domain.NodeConfigParser;

/**
 * 定义 SimpleNodeConfigParser 的执行上下文数据结构或业务契约。
 */
public class SimpleNodeConfigParser implements NodeConfigParser {

    /**
     * 执行 parse 对应的业务处理。
     * @param configJson configJson 参数
     * @param nodeId nodeId 参数
     * @return 处理后的结果
     */
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

    /**
     * 执行 scalar 对应的业务处理。
     * @param raw raw 参数
     * @return 处理后的结果
     */
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

    /**
     * 执行 unquote 对应的业务处理。
     * @param raw raw 参数
     * @return 处理后的结果
     */
    private String unquote(String raw) {
        if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }
}
