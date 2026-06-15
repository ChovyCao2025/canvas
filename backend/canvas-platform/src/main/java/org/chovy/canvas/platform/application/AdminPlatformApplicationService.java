package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.AdminPlatformFacade;
import org.chovy.canvas.platform.domain.AdminPlatformCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPlatformApplicationService implements AdminPlatformFacade {

    private final AdminPlatformCatalog catalog;

    public AdminPlatformApplicationService() {
        this(new AdminPlatformCatalog());
    }

    public AdminPlatformApplicationService(AdminPlatformCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<Map<String, Object>> users(Long tenantId) {
        return catalog.users(safeTenantId(tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createUser(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createUser(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateUser(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.updateUser(safeTenantId(tenantId), id, safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> disableUser(Long tenantId, Long id, String actor) {
        return catalog.disableUser(safeTenantId(tenantId), id, actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> projects(Long tenantId) {
        return catalog.projects(safeTenantId(tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createProject(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createProject(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> project(Long tenantId, Long projectId) {
        return catalog.project(safeTenantId(tenantId), projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateProject(Long tenantId, Long projectId, Map<String, Object> payload,
                                             String actor) {
        return catalog.updateProject(safeTenantId(tenantId), projectId, safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> disableProject(Long tenantId, Long projectId, String actor) {
        return catalog.disableProject(safeTenantId(tenantId), projectId, actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> projectMembers(Long tenantId, Long projectId) {
        return catalog.projectMembers(safeTenantId(tenantId), projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> setProjectMember(Long tenantId, Long projectId, Long userId,
                                                Map<String, Object> payload, String actor) {
        return catalog.setProjectMember(safeTenantId(tenantId), projectId, userId, safePayload(payload),
                actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> removeProjectMember(Long tenantId, Long projectId, Long userId) {
        return catalog.removeProjectMember(safeTenantId(tenantId), projectId, userId);
    }

    @Override
    public Map<String, Object> projectCanvases(Long tenantId, Long projectId, Integer page, Integer size) {
        return catalog.projectCanvases(safeTenantId(tenantId), projectId, page == null ? 1 : page,
                size == null ? 20 : size);
    }

    @Override
    public Map<String, Object> projectStats(Long tenantId, Long projectId) {
        return catalog.projectStats(safeTenantId(tenantId), projectId);
    }

    @Override
    public List<Map<String, Object>> systemOptions(Long tenantId, String category, Integer enabled, String keyword,
                                                   Long requestedTenantId) {
        return catalog.systemOptions(safeTenantId(tenantId), category, enabled, keyword, requestedTenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateSystemOption(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.updateSystemOption(safeTenantId(tenantId), id, safePayload(payload), actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> tenants() {
        return catalog.tenants();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createTenant(Map<String, Object> payload, String actor) {
        return catalog.createTenant(safePayload(payload), actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> disableTenant(Long id, String actor) {
        return catalog.disableTenant(id, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> activateTenant(Long id, String actor) {
        return catalog.activateTenant(id, actorOrDefault(actor));
    }

    @Override
    public Map<String, Object> tenantUsage(Long id) {
        return catalog.tenantUsage(id);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
}
