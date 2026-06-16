package org.chovy.canvas.platform.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 管理后台目录，保存租户、用户、项目、成员、画布和系统选项的演示数据。
 */
public class AdminPlatformCatalog {

    /**
     * 按租户标识保存的管理后台状态。
     */
    private final Map<Long, TenantAdminState> tenantStates = new LinkedHashMap<>();

    /**
     * 内存中的租户记录。
     */
    private final List<Map<String, Object>> tenants = new ArrayList<>();

    /**
     * 创建管理后台目录并写入默认租户数据。
     */
    public AdminPlatformCatalog() {
        tenants.add(newTenant(7L, "Default Tenant", "default", "PRO", "ACTIVE", "system"));
        seedTenant(7L);
        seedTenant(8L);
    }

    /**
     * 查询租户用户。
     *
     * @param tenantId 租户标识
     * @return 用户列表
     */
    public List<Map<String, Object>> users(Long tenantId) {
        return copies(state(tenantId).users);
    }

    /**
     * 创建租户用户。
     *
     * @param tenantId 租户标识
     * @param payload 用户创建参数
     * @param actor 操作者
     * @return 创建后的用户记录
     */
    public Map<String, Object> createUser(Long tenantId, Map<String, Object> payload, String actor) {
        required(payload, "username");
        TenantAdminState state = state(tenantId);
        Map<String, Object> user = copyWithoutPassword(payload);
        user.put("id", nextId(state.users, "id", 1001L));
        user.put("tenantId", tenantId);
        user.put("username", payload.get("username"));
        user.put("displayName", value(payload.get("displayName"), String.valueOf(payload.get("username"))));
        user.put("role", upper(value(payload.get("role"), "OPERATOR")));
        user.put("enabled", 1);
        user.put("createdBy", actor);
        state.users.add(user);
        return copy(user);
    }

    /**
     * 更新租户用户。
     *
     * @param tenantId 租户标识
     * @param id 用户标识
     * @param payload 用户更新参数
     * @param actor 操作者
     * @return 更新后的用户记录
     */
    public Map<String, Object> updateUser(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        Map<String, Object> user = find(state(tenantId).users, "id", id, "user not found");
        user.putAll(copyWithoutPassword(payload));
        if (payload.containsKey("role")) {
            user.put("role", upper(value(payload.get("role"), "")));
        }
        user.put("updatedBy", actor);
        return copy(user);
    }

    /**
     * 禁用租户用户。
     *
     * @param tenantId 租户标识
     * @param id 用户标识
     * @param actor 操作者
     * @return 禁用后的用户记录
     */
    public Map<String, Object> disableUser(Long tenantId, Long id, String actor) {
        Map<String, Object> user = find(state(tenantId).users, "id", id, "user not found");
        user.put("enabled", 0);
        user.put("updatedBy", actor);
        return copy(user);
    }

    /**
     * 查询租户项目。
     *
     * @param tenantId 租户标识
     * @return 项目列表
     */
    public List<Map<String, Object>> projects(Long tenantId) {
        return copies(state(tenantId).projects);
    }

    /**
     * 创建租户项目。
     *
     * @param tenantId 租户标识
     * @param payload 项目创建参数
     * @param actor 操作者
     * @return 创建后的项目记录
     */
    public Map<String, Object> createProject(Long tenantId, Map<String, Object> payload, String actor) {
        required(payload, "projectKey");
        TenantAdminState state = state(tenantId);
        Map<String, Object> project = new LinkedHashMap<>(payload);
        project.put("projectId", nextId(state.projects, "projectId", 2001L));
        project.put("tenantId", tenantId);
        project.put("projectKey", payload.get("projectKey"));
        project.put("projectName", value(payload.get("projectName"), String.valueOf(payload.get("projectKey"))));
        project.put("status", "ENABLED");
        project.put("createdBy", actor);
        state.projects.add(project);
        return copy(project);
    }

    /**
     * 查询单个项目。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @return 项目记录
     */
    public Map<String, Object> project(Long tenantId, Long projectId) {
        return copy(find(state(tenantId).projects, "projectId", projectId, "project not found"));
    }

    /**
     * 更新项目。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @param payload 项目更新参数
     * @param actor 操作者
     * @return 更新后的项目记录
     */
    public Map<String, Object> updateProject(Long tenantId, Long projectId, Map<String, Object> payload,
                                             String actor) {
        Map<String, Object> project = find(state(tenantId).projects, "projectId", projectId, "project not found");
        project.putAll(payload);
        project.put("updatedBy", actor);
        return copy(project);
    }

    /**
     * 禁用项目。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @param actor 操作者
     * @return 禁用后的项目记录
     */
    public Map<String, Object> disableProject(Long tenantId, Long projectId, String actor) {
        Map<String, Object> project = find(state(tenantId).projects, "projectId", projectId, "project not found");
        project.put("status", "DISABLED");
        project.put("updatedBy", actor);
        return copy(project);
    }

    /**
     * 查询项目成员。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @return 项目成员列表
     */
    public List<Map<String, Object>> projectMembers(Long tenantId, Long projectId) {
        return state(tenantId).members.stream()
                .filter(item -> Objects.equals(item.get("projectId"), projectId))
                .map(AdminPlatformCatalog::copy)
                .toList();
    }

    /**
     * 新增或覆盖项目成员。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @param userId 用户标识
     * @param payload 成员参数
     * @param actor 操作者
     * @return 成员记录
     */
    public Map<String, Object> setProjectMember(Long tenantId, Long projectId, Long userId, Map<String, Object> payload,
                                                String actor) {
        TenantAdminState state = state(tenantId);
        // 成员以项目和用户为唯一键，设置前先移除旧记录以保持幂等覆盖语义。
        state.members.removeIf(item -> Objects.equals(item.get("projectId"), projectId)
                && Objects.equals(item.get("userId"), userId));
        Map<String, Object> member = new LinkedHashMap<>(payload);
        member.put("tenantId", tenantId);
        member.put("projectId", projectId);
        member.put("userId", userId);
        member.put("role", upper(value(payload.get("role"), "MEMBER")));
        member.put("updatedBy", actor);
        state.members.add(member);
        return copy(member);
    }

    /**
     * 移除项目成员。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @param userId 用户标识
     * @return 移除结果
     */
    public Map<String, Object> removeProjectMember(Long tenantId, Long projectId, Long userId) {
        state(tenantId).members.removeIf(item -> Objects.equals(item.get("projectId"), projectId)
                && Objects.equals(item.get("userId"), userId));
        return Map.of("tenantId", tenantId, "projectId", projectId, "userId", userId, "removed", true);
    }

    /**
     * 查询项目画布分页数据。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @param page 页码
     * @param size 每页数量
     * @return 画布分页结果
     */
    public Map<String, Object> projectCanvases(Long tenantId, Long projectId, int page, int size) {
        List<Map<String, Object>> list = state(tenantId).canvases.stream()
                .filter(item -> Objects.equals(item.get("projectId"), projectId))
                .map(AdminPlatformCatalog::copy)
                .toList();
        return Map.of("tenantId", tenantId, "projectId", projectId, "page", page, "size", size,
                "total", (long) list.size(), "list", list);
    }

    /**
     * 查询项目统计信息。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @return 项目统计结果
     */
    public Map<String, Object> projectStats(Long tenantId, Long projectId) {
        long canvasCount = state(tenantId).canvases.stream()
                .filter(item -> Objects.equals(item.get("projectId"), projectId))
                .count();
        long memberCount = state(tenantId).members.stream()
                .filter(item -> Objects.equals(item.get("projectId"), projectId))
                .count();
        return Map.of("tenantId", tenantId, "projectId", projectId, "canvasCount", canvasCount,
                "memberCount", memberCount);
    }

    /**
     * 查询系统选项。
     *
     * @param tenantId 租户标识
     * @param category 选项分类
     * @param enabled 启用状态过滤值
     * @param keyword 关键字
     * @param requestedTenantId 请求指定的租户标识
     * @return 系统选项列表
     */
    public List<Map<String, Object>> systemOptions(Long tenantId, String category, Integer enabled, String keyword,
                                                   Long requestedTenantId) {
        return state(tenantId).options.stream()
                .filter(item -> matches(item, "category", category))
                .filter(item -> enabled == null || Objects.equals(item.get("enabled"), enabled))
                .filter(item -> keyword == null || value(item.get("optionKey"), "").contains(keyword)
                        || value(item.get("optionName"), "").toLowerCase(Locale.ROOT)
                                .contains(keyword.toLowerCase(Locale.ROOT)))
                .map(AdminPlatformCatalog::copy)
                .toList();
    }

    /**
     * 更新系统选项。
     *
     * @param tenantId 租户标识
     * @param id 选项标识
     * @param payload 选项更新参数
     * @param actor 操作者
     * @return 更新后的选项记录
     */
    public Map<String, Object> updateSystemOption(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        Map<String, Object> option = find(state(tenantId).options, "id", id, "system option not found");
        option.putAll(payload);
        option.put("updatedBy", actor);
        return copy(option);
    }

    /**
     * 查询全部租户。
     *
     * @return 租户列表
     */
    public List<Map<String, Object>> tenants() {
        return copies(tenants);
    }

    /**
     * 创建租户。
     *
     * @param payload 租户创建参数
     * @param actor 操作者
     * @return 创建后的租户记录
     */
    public Map<String, Object> createTenant(Map<String, Object> payload, String actor) {
        required(payload, "name");
        Map<String, Object> tenant = newTenant(nextId(tenants, "id", 7L), value(payload.get("name"), ""),
                value(payload.get("tenantKey"), "tenant-" + tenants.size()),
                upper(value(payload.get("planCode"), "FREE")), "ACTIVE", actor);
        tenant.put("quotaJson", payload.get("quotaJson"));
        tenants.add(tenant);
        seedTenant((Long) tenant.get("id"));
        return copy(tenant);
    }

    /**
     * 禁用租户。
     *
     * @param id 租户标识
     * @param actor 操作者
     * @return 禁用后的租户记录
     */
    public Map<String, Object> disableTenant(Long id, String actor) {
        Map<String, Object> tenant = find(tenants, "id", id, "tenant not found");
        tenant.put("status", "DISABLED");
        tenant.put("updatedBy", actor);
        return copy(tenant);
    }

    /**
     * 激活租户。
     *
     * @param id 租户标识
     * @param actor 操作者
     * @return 激活后的租户记录
     */
    public Map<String, Object> activateTenant(Long id, String actor) {
        Map<String, Object> tenant = find(tenants, "id", id, "tenant not found");
        tenant.put("status", "ACTIVE");
        tenant.put("updatedBy", actor);
        return copy(tenant);
    }

    /**
     * 查询租户使用量。
     *
     * @param id 租户标识
     * @return 租户使用量记录
     */
    public Map<String, Object> tenantUsage(Long id) {
        TenantAdminState state = state(id);
        return Map.of("tenantId", id, "userCount", (long) state.users.size(),
                "projectCount", (long) state.projects.size(), "canvasCount", (long) state.canvases.size());
    }

    /**
     * 为租户写入默认管理数据。
     *
     * @param tenantId 租户标识
     */
    private void seedTenant(Long tenantId) {
        TenantAdminState state = state(tenantId);
        if (!state.users.isEmpty()) {
            // 已有用户说明该租户已初始化，避免重复种子数据污染分页和统计。
            return;
        }
        state.users.add(new LinkedHashMap<>(Map.of(
                "id", 1001L, "tenantId", tenantId, "username", "operator-1",
                "displayName", "Operator", "role", "TENANT_ADMIN", "enabled", 1)));
        state.projects.add(new LinkedHashMap<>(Map.of(
                "projectId", 2001L, "tenantId", tenantId, "projectKey", "default",
                "projectName", "Default Project", "status", "ENABLED")));
        state.members.add(new LinkedHashMap<>(Map.of(
                "tenantId", tenantId, "projectId", 2001L, "userId", 1001L, "role", "OWNER")));
        state.canvases.add(new LinkedHashMap<>(Map.of(
                "canvasId", 4001L, "tenantId", tenantId, "projectId", 2001L, "canvasName", "Default Canvas")));
        state.options.add(new LinkedHashMap<>(Map.of(
                "id", 3001L, "tenantId", tenantId, "category", "canvas",
                "optionKey", "canvas.publish.review.required", "optionName", "Review required",
                "optionValue", "true", "enabled", 1)));
    }

    /**
     * 获取或创建租户管理状态。
     *
     * @param tenantId 租户标识
     * @return 租户管理状态
     */
    private TenantAdminState state(Long tenantId) {
        return tenantStates.computeIfAbsent(tenantId, ignored -> new TenantAdminState());
    }

    /**
     * 构造租户记录。
     *
     * @param id 租户标识
     * @param name 租户名称
     * @param key 租户键
     * @param plan 套餐编码
     * @param status 租户状态
     * @param actor 创建人
     * @return 租户记录
     */
    private static Map<String, Object> newTenant(Long id, String name, String key, String plan, String status,
                                                 String actor) {
        Map<String, Object> tenant = new LinkedHashMap<>();
        tenant.put("id", id);
        tenant.put("name", name);
        tenant.put("tenantKey", key);
        tenant.put("planCode", plan);
        tenant.put("status", status);
        tenant.put("createdBy", actor);
        return tenant;
    }

    /**
     * 根据已有记录计算下一个标识。
     *
     * @param rows 已有记录
     * @param key 标识字段
     * @param seed 起始种子
     * @return 下一个标识
     */
    private static Long nextId(List<Map<String, Object>> rows, String key, Long seed) {
        return rows.stream()
                .map(row -> row.get(key))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToLong(Number::longValue)
                .max()
                .orElse(seed - 1L) + 1L;
    }

    /**
     * 在列表中查找记录。
     *
     * @param rows 记录列表
     * @param key 匹配字段
     * @param value 匹配值
     * @param message 未找到时使用的异常消息
     * @return 匹配记录
     */
    private static Map<String, Object> find(List<Map<String, Object>> rows, String key, Object value, String message) {
        return rows.stream()
                .filter(row -> Objects.equals(row.get(key), value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    /**
     * 判断记录字段是否匹配可选过滤值。
     *
     * @param row 记录
     * @param key 字段键
     * @param expected 期望值
     * @return 匹配或未提供期望值时返回 true
     */
    private static boolean matches(Map<String, Object> row, String key, String expected) {
        return expected == null || Objects.equals(row.get(key), expected);
    }

    /**
     * 读取必填字段。
     *
     * @param payload 请求体
     * @param key 字段键
     * @return 字段文本
     */
    private static String required(Map<String, Object> payload, String key) {
        String value = value(payload.get(key), "");
        if (value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    /**
     * 将文本转为大写代码。
     *
     * @param value 原始文本
     * @return 大写文本；空值返回 null
     */
    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 将对象转为文本并提供默认值。
     *
     * @param value 原始对象
     * @param defaultValue 默认值
     * @return 文本值
     */
    private static String value(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    /**
     * 复制请求体并移除密码字段。
     *
     * @param payload 原始请求体
     * @return 不含密码的记录副本
     */
    private static Map<String, Object> copyWithoutPassword(Map<String, Object> payload) {
        Map<String, Object> copy = new LinkedHashMap<>(payload);
        copy.remove("password");
        return copy;
    }

    /**
     * 复制记录，避免调用方直接修改内部状态。
     *
     * @param row 原始记录
     * @return 复制后的记录
     */
    private static Map<String, Object> copy(Map<String, Object> row) {
        return new LinkedHashMap<>(row);
    }

    /**
     * 批量复制记录。
     *
     * @param rows 原始记录列表
     * @return 复制后的记录列表
     */
    private static List<Map<String, Object>> copies(List<Map<String, Object>> rows) {
        return rows.stream().map(AdminPlatformCatalog::copy).toList();
    }

    /**
     * 单租户管理后台状态容器。
     */
    private static final class TenantAdminState {
        /**
         * 租户用户记录。
         */
        private final List<Map<String, Object>> users = new ArrayList<>();

        /**
         * 租户项目记录。
         */
        private final List<Map<String, Object>> projects = new ArrayList<>();

        /**
         * 项目成员记录。
         */
        private final List<Map<String, Object>> members = new ArrayList<>();

        /**
         * 项目画布记录。
         */
        private final List<Map<String, Object>> canvases = new ArrayList<>();

        /**
         * 系统选项记录。
         */
        private final List<Map<String, Object>> options = new ArrayList<>();
    }
}
