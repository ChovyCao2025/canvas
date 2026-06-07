package org.chovy.canvas.domain.content;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.TenantContext;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MarketingContentSupport {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9_]*)\\s*}}");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private MarketingContentSupport() {
    }

    static Long requireTenantId(TenantContext tenant) {
        if (tenant == null || tenant.tenantId() == null) {
            throw new SecurityException("AUTH_003: missing tenant context");
        }
        return tenant.tenantId();
    }

    static String operator(TenantContext tenant, String fallback) {
        if (tenant != null && hasText(tenant.username())) {
            return tenant.username().trim();
        }
        return hasText(fallback) ? fallback.trim() : "operator";
    }

    static String requireText(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    static String normalizeKey(String value, String field) {
        String text = requireText(value, field).toLowerCase(Locale.ROOT);
        text = text.replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^[-_]+|[-_]+$)", "");
        if (text.isBlank() || !text.matches("[a-z0-9][a-z0-9_-]{0,127}")) {
            throw new IllegalArgumentException("invalid " + field + ": " + value);
        }
        return text;
    }

    static String normalizeSlug(String value) {
        if (!hasText(value)) {
            return null;
        }
        String text = value.trim().replaceFirst("^/+", "");
        return normalizeKey(text, "slug");
    }

    static String normalizeUpper(String value, String defaultValue, Set<String> allowed, String label) {
        String normalized = hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("unsupported " + label + " " + normalized);
        }
        return normalized;
    }

    static void validateHttpUrl(String url, String field) {
        String text = requireText(url, field);
        try {
            URI uri = URI.create(text);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new IllegalArgumentException(field + " must use http or https");
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("must use http or https")) {
                throw e;
            }
            throw new IllegalArgumentException(field + " must be a valid URL", e);
        }
    }

    static String tagsJson(ObjectMapper objectMapper, List<String> tags) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (tags != null) {
            for (String tag : tags) {
                if (hasText(tag)) {
                    normalized.add(tag.trim());
                }
            }
        }
        return toJson(objectMapper, List.copyOf(normalized), "tags");
    }

    static List<String> tags(ObjectMapper objectMapper, String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    static String objectJson(ObjectMapper objectMapper, Map<String, Object> value, String field) {
        return toJson(objectMapper, value == null ? Map.of() : value, field);
    }

    static String normalizeJsonObject(ObjectMapper objectMapper, String value, String defaultJson, String field) {
        JsonNode node = parseJson(objectMapper, value, defaultJson, field);
        if (!node.isObject()) {
            throw new IllegalArgumentException(field + " must be a JSON object");
        }
        return writeJson(objectMapper, node, field);
    }

    static String normalizeJsonArray(ObjectMapper objectMapper, String value, String defaultJson, String field) {
        JsonNode node = parseJson(objectMapper, value, defaultJson, field);
        if (!node.isArray()) {
            throw new IllegalArgumentException(field + " must be a JSON array");
        }
        return writeJson(objectMapper, node, field);
    }

    static List<String> variables(String... values) {
        LinkedHashSet<String> variables = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (!hasText(value)) {
                    continue;
                }
                Matcher matcher = VARIABLE_PATTERN.matcher(value);
                while (matcher.find()) {
                    variables.add(matcher.group(1));
                }
            }
        }
        return List.copyOf(variables);
    }

    static String render(String template, Map<String, Object> context, LinkedHashSet<String> missing) {
        if (template == null) {
            return null;
        }
        Map<String, Object> safeContext = context == null ? Map.of() : context;
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String variable = matcher.group(1);
            Object value = safeContext.get(variable);
            if (value == null) {
                missing.add(variable);
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(String.valueOf(value)));
            }
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    static String variablesJson(ObjectMapper objectMapper, List<String> variables) {
        return toJson(objectMapper, variables == null ? List.of() : variables, "variables");
    }

    static List<String> variablesFromJson(ObjectMapper objectMapper, String json) {
        return tags(objectMapper, json);
    }

    private static String toJson(ObjectMapper objectMapper, Object value, String field) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(field + " must be valid JSON", e);
        }
    }

    private static JsonNode parseJson(ObjectMapper objectMapper, String value, String defaultJson, String field) {
        String text = hasText(value) ? value.trim() : defaultJson;
        try {
            JsonNode node = objectMapper.readTree(text);
            if (node == null) {
                throw new IllegalArgumentException(field + " must be valid JSON");
            }
            return node;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(field + " must be valid JSON", e);
        }
    }

    private static String writeJson(ObjectMapper objectMapper, JsonNode node, String field) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(field + " must be valid JSON", e);
        }
    }
}
