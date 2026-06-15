package org.chovy.canvas.platform.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TestUserCatalog {

    private long nextSetId = 101L;
    private long nextUserId = 1002L;
    private final List<Map<String, Object>> sets = new ArrayList<>();
    private final List<Map<String, Object>> users = new ArrayList<>();

    public TestUserCatalog() {
        sets.add(set(100L, 0L, "Default rerun users", "Seed users for rerun preview", "system"));
        sets.add(set(200L, 7L, "Tenant 7 users", "Tenant scoped users", "operator-1"));
        users.add(user(1001L, 0L, 100L, "test-user-1", "Test User One",
                "{\"tier\":\"VIP\",\"city\":\"Shanghai\"}", "{\"coupon\":\"WELCOME\"}"));
        users.add(user(2001L, 7L, 200L, "tenant-user-1", "Tenant User One",
                "{\"tier\":\"REGULAR\"}", "{}"));
    }

    public List<Map<String, Object>> listSets(Long tenantId) {
        return sets.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .map(TestUserCatalog::copy)
                .toList();
    }

    public Map<String, Object> createSet(Long tenantId, String name, String description, String actor) {
        Map<String, Object> row = set(nextSetId++, tenantId, name, description, actor);
        sets.add(row);
        return copy(row);
    }

    public List<Map<String, Object>> listUsers(Long tenantId, Long setId) {
        return users.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("setId"), setId))
                .map(TestUserCatalog::copy)
                .toList();
    }

    public Map<String, Object> createUser(Long tenantId, Long setId, String userId, String displayName,
                                          Map<String, Object> profile, Map<String, Object> inputParams) {
        Map<String, Object> row = user(nextUserId++, tenantId, setId, userId,
                displayName == null || displayName.isBlank() ? userId : displayName.trim(),
                json(profile), json(inputParams));
        users.add(row);
        return copy(row);
    }

    public Map<String, Object> getUser(Long tenantId, Long id) {
        return users.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("id"), id))
                .findFirst()
                .map(TestUserCatalog::copy)
                .orElseThrow(() -> new IllegalArgumentException("test user not found"));
    }

    public Map<String, Object> preview(Long tenantId, Long id) {
        Map<String, Object> user = getUser(tenantId, id);
        return ordered(
                "id", user.get("id"),
                "userId", user.get("userId"),
                "displayName", user.get("displayName"),
                "profile", parseFlatJson(String.valueOf(user.get("profileJson"))),
                "inputParams", parseFlatJson(String.valueOf(user.get("inputParams"))),
                "context", ordered(
                        "tenantId", user.get("tenantId"),
                        "setId", user.get("setId"),
                        "source", "TEST_USER"));
    }

    private static Map<String, Object> set(Long id, Long tenantId, String name, String description, String createdBy) {
        return ordered(
                "id", id,
                "tenantId", tenantId,
                "name", name,
                "description", description,
                "createdBy", createdBy,
                "createdAt", "2026-01-01T00:00:00",
                "updatedAt", "2026-01-01T00:00:00");
    }

    private static Map<String, Object> user(Long id, Long tenantId, Long setId, String userId, String displayName,
                                            String profileJson, String inputParams) {
        return ordered(
                "id", id,
                "tenantId", tenantId,
                "setId", setId,
                "userId", userId,
                "displayName", displayName,
                "profileJson", profileJson,
                "inputParams", inputParams,
                "createdAt", "2026-01-01T00:00:00",
                "updatedAt", "2026-01-01T00:00:00");
    }

    private static String json(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return "{}";
        }
        return values.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":\"" + String.valueOf(entry.getValue()) + "\"")
                .reduce("{", (left, right) -> "{".equals(left) ? left + right : left + "," + right) + "}";
    }

    private static Map<String, Object> parseFlatJson(String json) {
        if (json == null || json.isBlank() || "{}".equals(json)) {
            return Map.of();
        }
        String body = json.trim();
        if (body.startsWith("{") && body.endsWith("}")) {
            body = body.substring(1, body.length() - 1);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        if (body.isBlank()) {
            return result;
        }
        for (String pair : body.split(",")) {
            String[] parts = pair.split(":", 2);
            if (parts.length == 2) {
                result.put(unquote(parts[0].trim()), unquote(parts[1].trim()));
            }
        }
        return result;
    }

    private static String unquote(String value) {
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private static Map<String, Object> ordered(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return result;
    }
}
