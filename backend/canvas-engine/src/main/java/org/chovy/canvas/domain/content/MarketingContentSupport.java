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

/**
 * MarketingContentSupport 业务组件。
 */
final class MarketingContentSupport {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9_]*)\\s*}}");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    /**
     * 执行 MarketingContentSupport 流程，围绕 marketing content support 完成校验、计算或结果组装。
     */
    private MarketingContentSupport() {
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenant tenant 参数，用于 requireTenantId 流程中的校验、计算或对象转换。
     * @return 返回 require tenant id 计算得到的数量、金额或指标值。
     */
    static Long requireTenantId(TenantContext tenant) {
        if (tenant == null || tenant.tenantId() == null) {
            throw new SecurityException("AUTH_003: missing tenant context");
        }
        return tenant.tenantId();
    }

    /**
     * 解析操作人标识。
     *
     * @param tenant tenant 参数，用于 operator 流程中的校验、计算或对象转换。
     * @param fallback fallback 参数，用于 operator 流程中的校验、计算或对象转换。
     * @return 返回 operator 生成的文本或业务键。
     */
    static String operator(TenantContext tenant, String fallback) {
        if (tenant != null && hasText(tenant.username())) {
            return tenant.username().trim();
        }
        return hasText(fallback) ? fallback.trim() : "operator";
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 require text 生成的文本或业务键。
     */
    static String requireText(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    static String normalizeKey(String value, String field) {
        String text = requireText(value, field).toLowerCase(Locale.ROOT);
        // Keys become URL/API-facing identifiers, so normalize to a stable lowercase slug instead of preserving display text.
        text = text.replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^[-_]+|[-_]+$)", "");
        if (text.isBlank() || !text.matches("[a-z0-9][a-z0-9_-]{0,127}")) {
            throw new IllegalArgumentException("invalid " + field + ": " + value);
        }
        return text;
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    static String normalizeSlug(String value) {
        if (!hasText(value)) {
            return null;
        }
        String text = value.trim().replaceFirst("^/+", "");
        return normalizeKey(text, "slug");
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @param allowed allowed 参数，用于 normalizeUpper 流程中的校验、计算或对象转换。
     * @param label label 参数，用于 normalizeUpper 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    static String normalizeUpper(String value, String defaultValue, Set<String> allowed, String label) {
        String normalized = hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("unsupported " + label + " " + normalized);
        }
        return normalized;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param url url 参数，用于 validateHttpUrl 流程中的校验、计算或对象转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     */
    static void validateHttpUrl(String url, String field) {
        String text = requireText(url, field);
        try {
            URI uri = URI.create(text);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new IllegalArgumentException(field + " must use http or https");
            }
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("must use http or https")) {
                throw e;
            }
            throw new IllegalArgumentException(field + " must be a valid URL", e);
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param tags tags 参数，用于 tagsJson 流程中的校验、计算或对象转换。
     * @return 返回 tags json 生成的文本或业务键。
     */
    static String tagsJson(ObjectMapper objectMapper, List<String> tags) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (tags != null) {
            for (String tag : tags) {
                if (hasText(tag)) {
                    normalized.add(tag.trim());
                }
            }
        }
        // LinkedHashSet removes duplicates while preserving caller order for stable UI rendering.
        return toJson(objectMapper, List.copyOf(normalized), "tags");
    }

    /**
     * 执行 tags 流程，围绕 tags 完成校验、计算或结果组装。
     *
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 tags 汇总后的集合、分页或映射视图。
     */
    static List<String> tags(ObjectMapper objectMapper, String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param String string 参数，用于 objectJson 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 object json 生成的文本或业务键。
     */
    static String objectJson(ObjectMapper objectMapper, Map<String, Object> value, String field) {
        return toJson(objectMapper, value == null ? Map.of() : value, field);
    }

    /**
     * 规范化输入值。
     *
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultJson JSON 字符串，承载结构化配置或明细。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    static String normalizeJsonObject(ObjectMapper objectMapper, String value, String defaultJson, String field) {
        JsonNode node = parseJson(objectMapper, value, defaultJson, field);
        if (!node.isObject()) {
            throw new IllegalArgumentException(field + " must be a JSON object");
        }
        return writeJson(objectMapper, node, field);
    }

    /**
     * 规范化输入值。
     *
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultJson JSON 字符串，承载结构化配置或明细。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    static String normalizeJsonArray(ObjectMapper objectMapper, String value, String defaultJson, String field) {
        JsonNode node = parseJson(objectMapper, value, defaultJson, field);
        if (!node.isArray()) {
            throw new IllegalArgumentException(field + " must be a JSON array");
        }
        return writeJson(objectMapper, node, field);
    }

    /**
     * 执行 variables 流程，围绕 variables 完成校验、计算或结果组装。
     *
     * @param values values 参数，用于 variables 流程中的校验、计算或对象转换。
     * @return 返回 variables 汇总后的集合、分页或映射视图。
     */
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
        // Variable extraction is deterministic so preview checks and stored variable metadata stay comparable.
        return List.copyOf(variables);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param template template 参数，用于 render 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 render 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param missing missing 参数，用于 render 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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
                // Preserve unresolved placeholders so reviewers can see exactly which token is missing.
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(rendered, Matcher.quoteReplacement(String.valueOf(value)));
            }
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param variables variables 参数，用于 variablesJson 流程中的校验、计算或对象转换。
     * @return 返回 variables json 生成的文本或业务键。
     */
    static String variablesJson(ObjectMapper objectMapper, List<String> variables) {
        return toJson(objectMapper, variables == null ? List.of() : variables, "variables");
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 variables from json 汇总后的集合、分页或映射视图。
     */
    static List<String> variablesFromJson(ObjectMapper objectMapper, String json) {
        return tags(objectMapper, json);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回组装或转换后的结果对象。
     */
    private static String toJson(ObjectMapper objectMapper, Object value, String field) {
        try {
            return objectMapper.writeValueAsString(value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(field + " must be valid JSON", e);
        }
    }

    /**
     * 解析并校验输入数据。
     *
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultJson JSON 字符串，承载结构化配置或明细。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static JsonNode parseJson(ObjectMapper objectMapper, String value, String defaultJson, String field) {
        String text = hasText(value) ? value.trim() : defaultJson;
        try {
            JsonNode node = objectMapper.readTree(text);
            if (node == null) {
                throw new IllegalArgumentException(field + " must be valid JSON");
            }
            return node;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(field + " must be valid JSON", e);
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param node node 参数，用于 writeJson 流程中的校验、计算或对象转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 write json 生成的文本或业务键。
     */
    private static String writeJson(ObjectMapper objectMapper, JsonNode node, String field) {
        try {
            return objectMapper.writeValueAsString(node);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(field + " must be valid JSON", e);
        }
    }
}
