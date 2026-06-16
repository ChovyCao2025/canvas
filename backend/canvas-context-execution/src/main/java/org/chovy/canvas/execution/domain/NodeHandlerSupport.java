package org.chovy.canvas.execution.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 定义 NodeHandlerSupport 的执行上下文数据结构或业务契约。
 */
final class NodeHandlerSupport {

    /**
     * 执行 NodeHandlerSupport 对应的业务处理。
     */
    private NodeHandlerSupport() {
    }

    /**
     * 执行 string 对应的业务处理。
     * @param value value 参数
     * @param fallback fallback 参数
     * @return 处理后的结果
     */
    static String string(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    /**
     * 执行 bool 对应的业务处理。
     * @param value value 参数
     * @return 处理后的结果
     */
    static boolean bool(Object value) {
        if (value instanceof Boolean parsed) {
            return parsed;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 执行 number 对应的业务处理。
     * @param value value 参数
     * @return 处理后的结果
     */
    static Number number(Object value) {
        if (value instanceof Number parsed) {
            return parsed;
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 执行 resolve 对应的业务处理。
     * @param context context 参数
     * @param key key 参数
     * @return 处理后的结果
     */
    static Object resolve(NodeExecutionContext context, String key) {
        String normalized = normalizeTemplate(key);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("payload.")) {
            return nestedValue(context.payload(), normalized.substring("payload.".length()));
        }
        if (normalized.startsWith("context.")) {
            return nestedValue(context.contextData(), normalized.substring("context.".length()));
        }
        if (context.contextData().containsKey(normalized)) {
            return context.contextData().get(normalized);
        }
        if (context.payload().containsKey(normalized)) {
            return context.payload().get(normalized);
        }
        Object contextValue = nestedValue(context.contextData(), normalized);
        return contextValue == null ? nestedValue(context.payload(), normalized) : contextValue;
    }

    /**
     * 执行 normalizeTemplate 对应的业务处理。
     * @param value value 参数
     */
    static String normalizeTemplate(String value) {
        String text = string(value, null);
        if (text == null) {
            return null;
        }
        if (text.startsWith("${") && text.endsWith("}") && text.length() > 3) {
            return text.substring(2, text.length() - 1).trim();
        }
        return text;
    }

    /**
     * 执行 listOfMaps 对应的业务处理。
     * @param value value 参数
     * @return 处理后的结果
     */
    static List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof Map<?, ?> rawMap) {
                result.add(stringObjectMap(rawMap));
            }
        }
        return List.copyOf(result);
    }

    /**
     * 执行 stringList 对应的业务处理。
     * @param value value 参数
     * @return 处理后的结果
     */
    static List<String> stringList(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        return rawList.stream()
                .map(item -> string(item, null))
                .filter(item -> item != null && !item.isBlank())
                .toList();
    }

    /**
     * 执行 upper 对应的业务处理。
     * @param value value 参数
     * @param fallback fallback 参数
     * @return 处理后的结果
     */
    static String upper(Object value, String fallback) {
        return string(value, fallback).toUpperCase(Locale.ROOT);
    }

    /**
     * 执行 collectionContains 对应的业务处理。
     * @param container container 参数
     * @param expected expected 参数
     */
    static boolean collectionContains(Object container, Object expected) {
        if (container instanceof Collection<?> collection) {
            return collection.contains(expected);
        }
        if (expected instanceof Collection<?> collection) {
            return collection.contains(container);
        }
        return container != null && expected != null && String.valueOf(container).contains(String.valueOf(expected));
    }

    /**
     * 执行 nestedValue 对应的业务处理。
     * @param source source 参数
     * @param path path 参数
     */
    static Object nestedValue(Map<String, Object> source, String path) {
        if (source == null || path == null || path.isBlank()) {
            return null;
        }
        if (source.containsKey(path)) {
            return source.get(path);
        }
        Object current = source;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }

    /**
     * 执行 stringObjectMap 对应的业务处理。
     * @param rawMap rawMap 参数
     * @return 处理后的结果
     */
    private static Map<String, Object> stringObjectMap(Map<?, ?> rawMap) {
        java.util.LinkedHashMap<String, Object> copy = new java.util.LinkedHashMap<>();
        rawMap.forEach((key, value) -> {
            if (key != null) {
                copy.put(String.valueOf(key), value);
            }
        });
        return Map.copyOf(copy);
    }
}
