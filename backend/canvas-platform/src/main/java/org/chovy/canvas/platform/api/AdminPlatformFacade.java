package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

public interface AdminPlatformFacade {

    List<Map<String, Object>> users(Long tenantId);

    Map<String, Object> createUser(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> updateUser(Long tenantId, Long id, Map<String, Object> payload, String actor);

    Map<String, Object> disableUser(Long tenantId, Long id, String actor);

    List<Map<String, Object>> projects(Long tenantId);

    Map<String, Object> createProject(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> project(Long tenantId, Long projectId);

    Map<String, Object> updateProject(Long tenantId, Long projectId, Map<String, Object> payload, String actor);

    Map<String, Object> disableProject(Long tenantId, Long projectId, String actor);

    List<Map<String, Object>> projectMembers(Long tenantId, Long projectId);

    Map<String, Object> setProjectMember(Long tenantId, Long projectId, Long userId, Map<String, Object> payload,
                                         String actor);

    Map<String, Object> removeProjectMember(Long tenantId, Long projectId, Long userId);

    Map<String, Object> projectCanvases(Long tenantId, Long projectId, Integer page, Integer size);

    Map<String, Object> projectStats(Long tenantId, Long projectId);

    List<Map<String, Object>> systemOptions(Long tenantId, String category, Integer enabled, String keyword,
                                            Long requestedTenantId);

    Map<String, Object> updateSystemOption(Long tenantId, Long id, Map<String, Object> payload, String actor);

    List<Map<String, Object>> tenants();

    Map<String, Object> createTenant(Map<String, Object> payload, String actor);

    Map<String, Object> disableTenant(Long id, String actor);

    Map<String, Object> activateTenant(Long id, String actor);

    Map<String, Object> tenantUsage(Long id);
}
