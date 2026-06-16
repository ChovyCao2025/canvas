package org.chovy.canvas.marketing.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 维护MarketingForm相关的内存业务目录。
 */
public class MarketingFormCatalog {

    private final List<Map<String, Object>> forms = new ArrayList<>();
    private final List<Map<String, Object>> submissions = new ArrayList<>();

    /**
     * 创建MarketingFormCatalog实例。
     */
    public MarketingFormCatalog() {
        forms.add(form(5001L, 7L, "lead-capture", "Lead Capture", "Capture newsletter leads",
                "[{\"name\":\"email\",\"type\":\"email\"}]", "{\"type\":\"tag\"}", "Thanks", true, "operator-1"));
        forms.add(form(5002L, 7L, "event-signup", "Event Signup", "Register event interest",
                "[{\"name\":\"phone\",\"type\":\"text\"}]", "{\"type\":\"workflow\"}", "Registered", false,
                "operator-1"));
        forms.add(form(5001L, 8L, "tenant-eight-form", "Tenant Eight Form", "Tenant scoped duplicate id",
                "[]", "{}", "OK", true, "operator-1"));
        submissions.add(submission(9101L, 7L, 5001L, "anon-1", Map.of("email", "ada@example.com")));
        submissions.add(submission(9102L, 7L, 5001L, "anon-2", Map.of("email", "grace@example.com")));
        submissions.add(submission(9103L, 7L, 5002L, "anon-3", Map.of("phone", "555-0100")));
        submissions.add(submission(9101L, 8L, 5001L, "tenant-8", Map.of("email", "tenant8@example.com")));
    }

    /**
     * 查询forms列表。
     */
    public synchronized List<Map<String, Object>> listForms(Long tenantId) {
        return forms.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .sorted(Comparator.comparing(row -> (Long) row.get("id")))
                .map(MarketingFormCatalog::copy)
                .toList();
    }

    /**
     * 返回form字段值。
     */
    public synchronized Map<String, Object> getForm(Long tenantId, Long id) {
        return copy(requireForm(tenantId, id));
    }

    /**
     * 创建form业务对象。
     */
    public synchronized Map<String, Object> createForm(Long tenantId, Map<String, Object> payload, String actor) {
        String name = string(payload, "name", null);
        if (name == null) {
            throw new IllegalArgumentException("name is required");
        }
        validateJson("fieldSchemaJson", string(payload, "fieldSchemaJson", "[]"));
        validateJson("submitActionJson", string(payload, "submitActionJson", "{}"));
        Long id = nextId(tenantId);
        String publicKey = string(payload, "publicKey", "form-" + id);
        Map<String, Object> form = form(id, tenantId, publicKey, name,
                string(payload, "description", null),
                string(payload, "fieldSchemaJson", "[]"),
                string(payload, "submitActionJson", "{}"),
                string(payload, "successMessage", "Submitted"),
                booleanValue(payload.get("active"), true),
                string(payload, "createdBy", actor));
        form.put("updatedBy", actor);
        forms.add(form);
        return copy(form);
    }

    /**
     * 更新form业务对象。
     */
    public synchronized Map<String, Object> updateForm(Long tenantId, Long id, Map<String, Object> payload,
                                                       String actor) {
        Map<String, Object> form = requireForm(tenantId, id);
        if (payload.containsKey("name") && string(payload, "name", null) == null) {
            throw new IllegalArgumentException("name is required");
        }
        if (payload.containsKey("fieldSchemaJson")) {
            validateJson("fieldSchemaJson", string(payload, "fieldSchemaJson", null));
        }
        if (payload.containsKey("submitActionJson")) {
            validateJson("submitActionJson", string(payload, "submitActionJson", null));
        }
        putIfPresent(form, payload, "publicKey");
        putIfPresent(form, payload, "name");
        putIfPresent(form, payload, "description");
        putIfPresent(form, payload, "fieldSchemaJson");
        putIfPresent(form, payload, "submitActionJson");
        putIfPresent(form, payload, "successMessage");
        if (payload.containsKey("active")) {
            boolean active = booleanValue(payload.get("active"), true);
            form.put("active", active);
            form.put("status", active ? "active" : "inactive");
        }
        form.put("updatedBy", actor);
        form.put("updatedAt", Instant.EPOCH.toString());
        return copy(form);
    }

    /**
     * 设置status字段值。
     */
    public synchronized Map<String, Object> setStatus(Long tenantId, Long id, Map<String, Object> payload,
                                                      String actor) {
        Map<String, Object> form = requireForm(tenantId, id);
        boolean active = booleanValue(payload.get("active"), false);
        form.put("active", active);
        form.put("status", active ? "active" : "inactive");
        form.put("updatedBy", actor);
        form.put("updatedAt", Instant.EPOCH.toString());
        return copy(form);
    }

    /**
     * 执行submissions业务操作。
     */
    public synchronized List<Map<String, Object>> submissions(Long tenantId, Long formId, int limit) {
        return submissions.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> formId == null || Objects.equals(row.get("formId"), formId))
                .sorted(Comparator.comparing(row -> (Long) row.get("id")))
                .limit(limit)
                .map(MarketingFormCatalog::copy)
                .toList();
    }

    /**
     * 校验并返回form必填值。
     */
    private Map<String, Object> requireForm(Long tenantId, Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("form id is required");
        }
        return forms.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("id"), id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("marketing form not found: " + id));
    }

    /**
     * 执行nextId业务操作。
     */
    private Long nextId(Long tenantId) {
        return forms.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .map(row -> (Long) row.get("id"))
                .max(Long::compareTo)
                .orElse(5000L) + 1L;
    }

    /**
     * 执行form业务操作。
     */
    private static Map<String, Object> form(Long id, Long tenantId, String publicKey, String name, String description,
                                            String fieldSchemaJson, String submitActionJson, String successMessage,
                                            boolean active, String createdBy) {
        return ordered(
                "id", id,
                "tenantId", tenantId,
                "publicKey", publicKey,
                "name", name,
                "description", description,
                "fieldSchemaJson", fieldSchemaJson,
                "submitActionJson", submitActionJson,
                "successMessage", successMessage,
                "active", active,
                "status", active ? "active" : "inactive",
                "createdBy", createdBy,
                "createdAt", Instant.EPOCH.toString());
    }

    /**
     * 执行submission业务操作。
     */
    private static Map<String, Object> submission(Long id, Long tenantId, Long formId, String anonymousId,
                                                  Map<String, Object> response) {
        return ordered(
                "id", id,
                "tenantId", tenantId,
                "formId", formId,
                "anonymousId", anonymousId,
                "response", response,
                "submittedAt", Instant.EPOCH.toString());
    }

    /**
     * 执行putIfPresent业务操作。
     */
    private static void putIfPresent(Map<String, Object> target, Map<String, Object> payload, String key) {
        if (payload.containsKey(key)) {
            target.put(key, payload.get(key));
        }
    }

    /**
     * 执行string业务操作。
     */
    private static String string(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return String.valueOf(value).trim();
    }

    /**
     * 执行booleanValue业务操作。
     */
    private static boolean booleanValue(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 执行validateJson业务操作。
     */
    private static void validateJson(String field, String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (!(trimmed.startsWith("{") && trimmed.endsWith("}"))
                && !(trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            throw new IllegalArgumentException(field + " must be JSON");
        }
    }

    /**
     * 执行copy业务操作。
     */
    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    /**
     * 执行ordered业务操作。
     */
    private static Map<String, Object> ordered(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return result;
    }
}
