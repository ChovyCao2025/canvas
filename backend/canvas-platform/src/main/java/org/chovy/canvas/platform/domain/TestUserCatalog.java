package org.chovy.canvas.platform.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 测试用户目录，保存可重复使用的测试用户集合和用户样本。
 */
public class TestUserCatalog {

    /**
     * 下一个测试用户集合标识。
     */
    private long nextSetId = 101L;

    /**
     * 下一个测试用户记录标识。
     */
    private long nextUserId = 1002L;

    /**
     * 内存中的测试用户集合。
     */
    private final List<Map<String, Object>> sets = new ArrayList<>();

    /**
     * 内存中的测试用户记录。
     */
    private final List<Map<String, Object>> users = new ArrayList<>();

    /**
     * 创建测试用户目录并写入固定样本。
     */
    public TestUserCatalog() {
        sets.add(set(100L, 0L, "Default rerun users", "Seed users for rerun preview", "system"));
        sets.add(set(200L, 7L, "Tenant 7 users", "Tenant scoped users", "operator-1"));
        users.add(user(1001L, 0L, 100L, "test-user-1", "Test User One",
                "{\"tier\":\"VIP\",\"city\":\"Shanghai\"}", "{\"coupon\":\"WELCOME\"}"));
        users.add(user(2001L, 7L, 200L, "tenant-user-1", "Tenant User One",
                "{\"tier\":\"REGULAR\"}", "{}"));
    }

    /**
     * 查询租户下的测试用户集合。
     *
     * @param tenantId 租户标识
     * @return 测试用户集合列表
     */
    public List<Map<String, Object>> listSets(Long tenantId) {
        return sets.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .map(TestUserCatalog::copy)
                .toList();
    }

    /**
     * 创建测试用户集合。
     *
     * @param tenantId 租户标识
     * @param name 集合名称
     * @param description 集合描述
     * @param actor 操作者
     * @return 创建后的集合记录
     */
    public Map<String, Object> createSet(Long tenantId, String name, String description, String actor) {
        Map<String, Object> row = set(nextSetId++, tenantId, name, description, actor);
        sets.add(row);
        return copy(row);
    }

    /**
     * 查询集合内测试用户。
     *
     * @param tenantId 租户标识
     * @param setId 测试用户集合标识
     * @return 测试用户列表
     */
    public List<Map<String, Object>> listUsers(Long tenantId, Long setId) {
        return users.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("setId"), setId))
                .map(TestUserCatalog::copy)
                .toList();
    }

    /**
     * 创建测试用户。
     *
     * @param tenantId 租户标识
     * @param setId 测试用户集合标识
     * @param userId 业务用户标识
     * @param displayName 展示名称
     * @param profile 用户画像
     * @param inputParams 输入参数
     * @return 创建后的测试用户记录
     */
    public Map<String, Object> createUser(Long tenantId, Long setId, String userId, String displayName,
                                          Map<String, Object> profile, Map<String, Object> inputParams) {
        Map<String, Object> row = user(nextUserId++, tenantId, setId, userId,
                displayName == null || displayName.isBlank() ? userId : displayName.trim(),
                json(profile), json(inputParams));
        users.add(row);
        return copy(row);
    }

    /**
     * 查询单个测试用户。
     *
     * @param tenantId 租户标识
     * @param id 测试用户记录标识
     * @return 测试用户记录
     */
    public Map<String, Object> getUser(Long tenantId, Long id) {
        return users.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("id"), id))
                .findFirst()
                .map(TestUserCatalog::copy)
                .orElseThrow(() -> new IllegalArgumentException("test user not found"));
    }

    /**
     * 生成测试用户预览数据。
     *
     * @param tenantId 租户标识
     * @param id 测试用户记录标识
     * @return 测试用户预览结果
     */
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

    /**
     * 构造测试用户集合记录。
     *
     * @param id 集合标识
     * @param tenantId 租户标识
     * @param name 集合名称
     * @param description 集合描述
     * @param createdBy 创建人
     * @return 集合记录
     */
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

    /**
     * 构造测试用户记录。
     *
     * @param id 记录标识
     * @param tenantId 租户标识
     * @param setId 集合标识
     * @param userId 业务用户标识
     * @param displayName 展示名称
     * @param profileJson 用户画像 JSON
     * @param inputParams 输入参数 JSON
     * @return 测试用户记录
     */
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

    /**
     * 将扁平 Map 转换为简单 JSON 字符串。
     *
     * @param values 原始 Map
     * @return 简单 JSON 字符串
     */
    private static String json(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return "{}";
        }
        return values.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\":\"" + String.valueOf(entry.getValue()) + "\"")
                .reduce("{", (left, right) -> "{".equals(left) ? left + right : left + "," + right) + "}";
    }

    /**
     * 解析简单扁平 JSON 字符串。
     *
     * @param json JSON 文本
     * @return 解析后的 Map
     */
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

    /**
     * 去除字符串两侧的双引号。
     *
     * @param value 原始字符串
     * @return 去引号后的字符串
     */
    private static String unquote(String value) {
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * 复制记录，避免调用方直接修改内部状态。
     *
     * @param source 原始记录
     * @return 复制后的记录
     */
    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    /**
     * 按参数顺序构造有序 Map。
     *
     * @param pairs 键值交替排列的参数
     * @return 有序 Map
     */
    private static Map<String, Object> ordered(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return result;
    }
}
