package org.chovy.canvas.marketing.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MetaOptionView;

public class MetaOptionCatalog {

    private final Map<String, List<MetaOptionView>> optionsByCategory = new LinkedHashMap<>();
    private final Map<String, List<MetaOptionView>> groupsByExperimentKey = new LinkedHashMap<>();

    public MetaOptionCatalog() {
        optionsByCategory.put("coupon_type", List.of(
                option("DISCOUNT", "Discount coupon"),
                option("CASH", "Cash coupon")));
        optionsByCategory.put("reach_scene", List.of(
                option("WELCOME", "Welcome journey"),
                option("RETENTION", "Retention journey")));
        optionsByCategory.put("mq_topic_legacy", List.of(
                option("canvas.user.changed", "User changed"),
                option("canvas.order.paid", "Order paid")));
        optionsByCategory.put("biz_line", List.of(
                option("retail", "Retail"),
                option("finance", "Finance")));
        optionsByCategory.put("biz_line_api", List.of(
                option("send-coupon", "Send coupon"),
                option("query-profile", "Query profile")));
        optionsByCategory.put("behavior_strategy_type", List.of(
                option("ALLOW", "Allow"),
                option("BLOCK", "Block")));
        optionsByCategory.put("message_code_in_app", List.of(
                option("IN_APP_WELCOME", "In-app welcome"),
                option("IN_APP_RETENTION", "In-app retention")));
        optionsByCategory.put("message_code_mq", List.of(
                option("MQ_COUPON_GRANTED", "Coupon granted"),
                option("MQ_PROFILE_UPDATED", "Profile updated")));
        optionsByCategory.put("channel", List.of(
                option("sms", "SMS"),
                option("email", "Email")));

        groupsByExperimentKey.put("checkout-test", List.of(
                option("A", "Control"),
                option("B", "Treatment")));
    }

    public List<MetaOptionView> options(Long tenantId, String category) {
        return List.copyOf(optionsByCategory.getOrDefault(normalize(category), List.of()));
    }

    public Map<String, List<MetaOptionView>> optionsBatch(Long tenantId, List<String> categories) {
        Map<String, List<MetaOptionView>> result = new LinkedHashMap<>();
        if (categories == null) {
            return result;
        }
        for (String category : categories) {
            String normalized = normalize(category);
            result.putIfAbsent(normalized, options(tenantId, normalized));
        }
        return result;
    }

    public List<MetaOptionView> abExperiments(Long tenantId) {
        return List.of(option("checkout-test", "Checkout test"));
    }

    public List<MetaOptionView> abExperimentGroups(Long tenantId, String experimentKey) {
        return List.copyOf(groupsByExperimentKey.getOrDefault(normalize(experimentKey), List.of()));
    }

    public List<MetaOptionView> bizLines(Long tenantId) {
        return options(tenantId, "biz_line");
    }

    public List<MetaOptionView> bizLineApis(Long tenantId, String bizLineKey) {
        return options(tenantId, "biz_line_api");
    }

    public List<MetaOptionView> aiProviders(Long tenantId) {
        return List.of(option("11", "OpenAI (openai)"));
    }

    public List<MetaOptionView> aiTemplates(Long tenantId) {
        return List.of(option("9001", "Winback (MARKETING)"));
    }

    public List<MetaOptionView> aiModels(Long tenantId, Long providerId) {
        if (providerId != null && providerId != 11L) {
            return List.of();
        }
        return List.of(option("gpt-4.1-mini", "GPT-4.1 mini"));
    }

    public List<MetaOptionView> identityTypes(Integer allowImport) {
        if (allowImport != null && allowImport == 0) {
            return List.of(option("phone", "Phone"));
        }
        return List.of(
                option("phone", "Phone"),
                option("email", "Email"));
    }

    public List<Map<String, Object>> apiDefinitions() {
        return List.of(schemaOption(
                "send-coupon",
                "Send coupon",
                "[{\"name\":\"couponId\",\"type\":\"STRING\"}]",
                1));
    }

    public List<Map<String, Object>> eventDefinitions() {
        return List.of(schemaOption(
                "user.created",
                "User created",
                "[{\"name\":\"source\",\"type\":\"STRING\"}]",
                null));
    }

    public List<Map<String, Object>> contextFields() {
        return List.of(field("userId", "User ID", "STRING", "trigger"));
    }

    public List<Map<String, Object>> canvasContextFields(
            List<String> eventCodes,
            List<String> apiKeys,
            List<String> outputPrefixes) {
        java.util.ArrayList<Map<String, Object>> fields = new java.util.ArrayList<>();
        fields.add(field("userId", "User ID", "STRING", "trigger"));
        if (eventCodes != null && !eventCodes.isEmpty()) {
            fields.add(field("source", "Source (User created)", "STRING", "EVENT_TRIGGER"));
        }
        if (apiKeys != null && !apiKeys.isEmpty()) {
            String prefix = outputPrefixes != null && !outputPrefixes.isEmpty()
                    && outputPrefixes.getFirst() != null && !outputPrefixes.getFirst().isBlank()
                    ? outputPrefixes.getFirst() + "."
                    : "";
            fields.add(field(prefix + "status", "Status (Send coupon)", "STRING", "API_CALL"));
        }
        return List.copyOf(fields);
    }

    public List<Map<String, Object>> mqDefinitions() {
        return List.of(schemaOption(
                "MQ_COUPON_GRANTED",
                "Coupon granted",
                "[{\"name\":\"couponId\",\"type\":\"STRING\"}]",
                null));
    }

    public List<MetaOptionView> taggerTags(String type) {
        if ("online".equalsIgnoreCase(normalize(type))) {
            return List.of(option("active_session", "Active session"));
        }
        return List.of(option("market_identity", "Market identity"));
    }

    public List<MetaOptionView> taggerTagValues(String tagCode) {
        if ("market_identity".equals(normalize(tagCode))) {
            return List.of(
                    option("new_user", "New user"),
                    option("returning_user", "Returning user"));
        }
        return List.of();
    }

    private static Map<String, Object> schemaOption(
            String value,
            String label,
            String requestSchema,
            Integer includeContextPayload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value", value);
        result.put("label", label);
        result.put("requestSchema", requestSchema == null ? "[]" : requestSchema);
        if (includeContextPayload != null) {
            result.put("includeContextPayload", includeContextPayload);
        }
        return result;
    }

    private static Map<String, Object> field(String key, String name, String dataType, String sourceNodeType) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fieldKey", key);
        result.put("fieldName", name);
        result.put("dataType", dataType);
        result.put("sourceNodeType", sourceNodeType);
        return result;
    }

    private static MetaOptionView option(String value, String label) {
        return new MetaOptionView(value, label);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
