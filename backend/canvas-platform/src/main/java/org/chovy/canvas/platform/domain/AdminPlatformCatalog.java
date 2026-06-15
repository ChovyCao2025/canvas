package org.chovy.canvas.platform.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AdminPlatformCatalog {

    private final Map<Long, TenantAdminState> tenantStates = new LinkedHashMap<>();
    private final List<Map<String, Object>> tenants = new ArrayList<>();

    public AdminPlatformCatalog() {
        tenants.add(newTenant(7L, "Default Tenant", "default", "PRO", "ACTIVE", "system"));
        seedTenant(7L);
        seedTenant(8L);
    }

    public List<Map<String, Object>> users(Long tenantId) {
        return copies(state(tenantId).users);
    }

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

    public Map<String, Object> updateUser(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        Map<String, Object> user = find(state(tenantId).users, "id", id, "user not found");
        user.putAll(copyWithoutPassword(payload));
        if (payload.containsKey("role")) {
            user.put("role", upper(value(payload.get("role"), "")));
        }
        user.put("updatedBy", actor);
        return copy(user);
    }

    public Map<String, Object> disableUser(Long tenantId, Long id, String actor) {
        Map<String, Object> user = find(state(tenantId).users, "id", id, "user not found");
        user.put("enabled", 0);
        user.put("updatedBy", actor);
        return copy(user);
    }

    public List<Map<String, Object>> projects(Long tenantId) {
        return copies(state(tenantId).projects);
    }

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

    public Map<String, Object> project(Long tenantId, Long projectId) {
        return copy(find(state(tenantId).projects, "projectId", projectId, "project not found"));
    }

    public Map<String, Object> updateProject(Long tenantId, Long projectId, Map<String, Object> payload,
                                             String actor) {
        Map<String, Object> project = find(state(tenantId).projects, "projectId", projectId, "project not found");
        project.putAll(payload);
        project.put("updatedBy", actor);
        return copy(project);
    }

    public Map<String, Object> disableProject(Long tenantId, Long projectId, String actor) {
        Map<String, Object> project = find(state(tenantId).projects, "projectId", projectId, "project not found");
        project.put("status", "DISABLED");
        project.put("updatedBy", actor);
        return copy(project);
    }

    public List<Map<String, Object>> projectMembers(Long tenantId, Long projectId) {
        return state(tenantId).members.stream()
                .filter(item -> Objects.equals(item.get("projectId"), projectId))
                .map(AdminPlatformCatalog::copy)
                .toList();
    }

    public Map<String, Object> setProjectMember(Long tenantId, Long projectId, Long userId, Map<String, Object> payload,
                                                String actor) {
        TenantAdminState state = state(tenantId);
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

    public Map<String, Object> removeProjectMember(Long tenantId, Long projectId, Long userId) {
        state(tenantId).members.removeIf(item -> Objects.equals(item.get("projectId"), projectId)
                && Objects.equals(item.get("userId"), userId));
        return Map.of("tenantId", tenantId, "projectId", projectId, "userId", userId, "removed", true);
    }

    public Map<String, Object> projectCanvases(Long tenantId, Long projectId, int page, int size) {
        List<Map<String, Object>> list = state(tenantId).canvases.stream()
                .filter(item -> Objects.equals(item.get("projectId"), projectId))
                .map(AdminPlatformCatalog::copy)
                .toList();
        return Map.of("tenantId", tenantId, "projectId", projectId, "page", page, "size", size,
                "total", (long) list.size(), "list", list);
    }

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

    public Map<String, Object> updateSystemOption(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        Map<String, Object> option = find(state(tenantId).options, "id", id, "system option not found");
        option.putAll(payload);
        option.put("updatedBy", actor);
        return copy(option);
    }

    public List<Map<String, Object>> tenants() {
        return copies(tenants);
    }

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

    public Map<String, Object> disableTenant(Long id, String actor) {
        Map<String, Object> tenant = find(tenants, "id", id, "tenant not found");
        tenant.put("status", "DISABLED");
        tenant.put("updatedBy", actor);
        return copy(tenant);
    }

    public Map<String, Object> activateTenant(Long id, String actor) {
        Map<String, Object> tenant = find(tenants, "id", id, "tenant not found");
        tenant.put("status", "ACTIVE");
        tenant.put("updatedBy", actor);
        return copy(tenant);
    }

    public Map<String, Object> tenantUsage(Long id) {
        TenantAdminState state = state(id);
        return Map.of("tenantId", id, "userCount", (long) state.users.size(),
                "projectCount", (long) state.projects.size(), "canvasCount", (long) state.canvases.size());
    }

    private void seedTenant(Long tenantId) {
        TenantAdminState state = state(tenantId);
        if (!state.users.isEmpty()) {
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

    private TenantAdminState state(Long tenantId) {
        return tenantStates.computeIfAbsent(tenantId, ignored -> new TenantAdminState());
    }

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

    private static Long nextId(List<Map<String, Object>> rows, String key, Long seed) {
        return rows.stream()
                .map(row -> row.get(key))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToLong(Number::longValue)
                .max()
                .orElse(seed - 1L) + 1L;
    }

    private static Map<String, Object> find(List<Map<String, Object>> rows, String key, Object value, String message) {
        return rows.stream()
                .filter(row -> Objects.equals(row.get(key), value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    private static boolean matches(Map<String, Object> row, String key, String expected) {
        return expected == null || Objects.equals(row.get(key), expected);
    }

    private static String required(Map<String, Object> payload, String key) {
        String value = value(payload.get(key), "");
        if (value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String value(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private static Map<String, Object> copyWithoutPassword(Map<String, Object> payload) {
        Map<String, Object> copy = new LinkedHashMap<>(payload);
        copy.remove("password");
        return copy;
    }

    private static Map<String, Object> copy(Map<String, Object> row) {
        return new LinkedHashMap<>(row);
    }

    private static List<Map<String, Object>> copies(List<Map<String, Object>> rows) {
        return rows.stream().map(AdminPlatformCatalog::copy).toList();
    }

    private static final class TenantAdminState {
        private final List<Map<String, Object>> users = new ArrayList<>();
        private final List<Map<String, Object>> projects = new ArrayList<>();
        private final List<Map<String, Object>> members = new ArrayList<>();
        private final List<Map<String, Object>> canvases = new ArrayList<>();
        private final List<Map<String, Object>> options = new ArrayList<>();
    }
}
