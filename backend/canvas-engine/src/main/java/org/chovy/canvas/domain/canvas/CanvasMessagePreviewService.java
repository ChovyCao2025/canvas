package org.chovy.canvas.domain.canvas;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.DataMaskingUtil;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dto.canvas.MessagePreviewReq;
import org.chovy.canvas.dto.canvas.MessagePreviewResp;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CanvasMessagePreviewService {

    private static final List<String> CONTENT_FIELDS = List.of(
            "subject", "previewText", "title", "body", "content",
            "imageUrl", "clickUrl", "fromName", "fromEmail");
    private static final List<String> KNOWN_CONFIG_FIELDS = List.of(
            "channel", "templateId", "template_id", "variables", "variablesMapping",
            "subject", "previewText", "title", "body", "content",
            "imageUrl", "clickUrl", "fromName", "fromEmail");
    private static final Pattern DOLLAR_TOKEN =
            Pattern.compile("\\$\\.?([A-Za-z_][A-Za-z0-9_.-]*)");
    private static final Pattern HANDLEBARS_TOKEN =
            Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_.-]*)\\s*}}");
    private static final Set<String> PREVIEW_SENSITIVE_KEYS = Set.of(
            "phone", "mobile", "phoneNumber", "mobileNumber",
            "idCard", "idNumber", "identityCard",
            "bankCard", "cardNumber",
            "password", "passwd", "pwd",
            "token", "accessToken", "refreshToken",
            "secret", "apiKey", "authorization",
            "cookie", "session", "credential");

    private final ObjectMapper objectMapper;

    public MessagePreviewResp preview(MessagePreviewReq req) {
        Map<String, Object> root = parseGraph(req.graphJson());
        Map<String, Object> node = findNode(root, req.nodeId());
        if (!NodeType.SEND_MESSAGE.equals(nodeType(node))) {
            throw new IllegalArgumentException("Message preview requires a SEND_MESSAGE node: " + req.nodeId());
        }

        Map<String, Object> config = mergedConfig(node);
        Map<String, Object> context = previewContext(req);
        List<String> warnings = new ArrayList<>();
        warnings.add("PREVIEW_ONLY_NO_SEND");

        Map<String, Object> content = new LinkedHashMap<>();
        for (String field : CONTENT_FIELDS) {
            if (config.containsKey(field)) {
                content.put(field, resolveAny(config.get(field), context, warnings));
            }
        }

        Map<String, Object> variables = resolveVariables(
                config.getOrDefault("variables", config.get("variablesMapping")),
                context,
                warnings);

        return new MessagePreviewResp(
                string(config, "channel", "EMAIL").toUpperCase(),
                string(config, "templateId", string(config, "template_id", null)),
                maskedMap(content),
                maskedMap(variables),
                List.copyOf(new LinkedHashSet<>(warnings)));
    }

    private Map<String, Object> parseGraph(String graphJson) {
        if (graphJson == null || graphJson.isBlank()) {
            throw new IllegalArgumentException("Message preview requires graphJson");
        }
        try {
            return objectMapper.readValue(graphJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Message preview graphJson parse failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findNode(Map<String, Object> root, String nodeId) {
        Object rawNodes = root.get("nodes");
        if (!(rawNodes instanceof List<?> nodes)) {
            throw new IllegalArgumentException("Message preview graph requires nodes");
        }
        if (nodeId != null && !nodeId.isBlank()) {
            for (Object candidate : nodes) {
                if (candidate instanceof Map<?, ?> map && nodeId.equals(String.valueOf(map.get("id")))) {
                    return (Map<String, Object>) map;
                }
            }
            throw new IllegalArgumentException("Message preview node not found: " + nodeId);
        }

        List<Map<String, Object>> sendNodes = new ArrayList<>();
        for (Object candidate : nodes) {
            if (candidate instanceof Map<?, ?> map && NodeType.SEND_MESSAGE.equals(nodeType(map))) {
                sendNodes.add((Map<String, Object>) map);
            }
        }
        if (sendNodes.size() != 1) {
            throw new IllegalArgumentException("Message preview requires one selected SEND_MESSAGE node");
        }
        return sendNodes.get(0);
    }

    private String nodeType(Map<?, ?> node) {
        Object direct = node.get("type");
        if (direct != null) {
            return String.valueOf(direct);
        }
        Map<String, Object> data = objectMap(node.get("data"));
        Object dataType = data.getOrDefault("type", data.get("typeKey"));
        return dataType == null ? null : String.valueOf(dataType);
    }

    private Map<String, Object> mergedConfig(Map<String, Object> node) {
        Map<String, Object> config = new LinkedHashMap<>();
        Map<String, Object> data = objectMap(node.get("data"));
        merge(config, objectMap(data.get("bizConfig")));
        merge(config, objectMap(data.get("config")));
        copyKnown(data, config);
        merge(config, objectMap(node.get("bizConfig")));
        merge(config, objectMap(node.get("config")));
        copyKnown(node, config);
        return config;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        ((Map<Object, Object>) map).forEach((key, item) -> {
            if (key != null) {
                result.put(String.valueOf(key), item);
            }
        });
        return result;
    }

    private void merge(Map<String, Object> target, Map<String, Object> source) {
        target.putAll(source);
    }

    private void copyKnown(Map<String, Object> source, Map<String, Object> target) {
        for (String field : KNOWN_CONFIG_FIELDS) {
            if (source.containsKey(field)) {
                target.put(field, source.get(field));
            }
        }
    }

    private Map<String, Object> previewContext(MessagePreviewReq req) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (req.context() != null) {
            context.putAll(req.context());
        }
        if (req.userId() != null) {
            context.putIfAbsent("userId", req.userId());
        }
        if (req.canvasId() != null) {
            context.putIfAbsent("canvasId", req.canvasId());
        }
        return context;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveVariables(Object raw, Map<String, Object> context, List<String> warnings) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        ((Map<Object, Object>) map).forEach((key, value) -> {
            if (key != null) {
                variables.put(String.valueOf(key), resolveAny(value, context, warnings));
            }
        });
        return variables;
    }

    @SuppressWarnings("unchecked")
    private Object resolveAny(Object value, Map<String, Object> context, List<String> warnings) {
        if (value instanceof String text) {
            return resolveText(text, context, warnings);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> resolved = new LinkedHashMap<>();
            ((Map<Object, Object>) map).forEach((key, item) -> {
                if (key != null) {
                    resolved.put(String.valueOf(key), resolveAny(item, context, warnings));
                }
            });
            return resolved;
        }
        if (value instanceof List<?> list) {
            List<Object> resolved = new ArrayList<>(list.size());
            for (Object item : list) {
                resolved.add(resolveAny(item, context, warnings));
            }
            return resolved;
        }
        return value;
    }

    private Object resolveText(String text, Map<String, Object> context, List<String> warnings) {
        String exact = exactVariableName(text);
        if (exact != null) {
            Object value = contextValue(context, exact);
            if (value == null) {
                addMissing(warnings, exact);
                return text;
            }
            return value;
        }
        String withHandlebars = replaceTokens(text, HANDLEBARS_TOKEN, context, warnings);
        return replaceTokens(withHandlebars, DOLLAR_TOKEN, context, warnings);
    }

    private String exactVariableName(String text) {
        Matcher handlebars = HANDLEBARS_TOKEN.matcher(text);
        if (handlebars.matches()) {
            return handlebars.group(1);
        }
        Matcher dollar = DOLLAR_TOKEN.matcher(text);
        if (dollar.matches()) {
            return dollar.group(1);
        }
        return null;
    }

    private String replaceTokens(String text, Pattern pattern, Map<String, Object> context, List<String> warnings) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String field = matcher.group(1);
            Object value = contextValue(context, field);
            if (value == null) {
                addMissing(warnings, field);
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(value)));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private Object contextValue(Map<String, Object> context, String field) {
        if (context.containsKey(field)) {
            return context.get(field);
        }
        Object current = context;
        for (String part : field.split("\\.")) {
            if (!(current instanceof Map<?, ?> map) || !map.containsKey(part)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(part);
        }
        return current;
    }

    private void addMissing(List<String> warnings, String field) {
        warnings.add("MISSING_VARIABLE:" + field);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> maskedMap(Map<String, Object> value) {
        Object masked = DataMaskingUtil.maskObject(value, PREVIEW_SENSITIVE_KEYS);
        return masked instanceof Map<?, ?> ? (Map<String, Object>) masked : Map.of();
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString().trim();
    }
}
