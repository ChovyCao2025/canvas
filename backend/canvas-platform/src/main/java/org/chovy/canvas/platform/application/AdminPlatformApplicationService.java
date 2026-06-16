package org.chovy.canvas.platform.application;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.api.AdminPlatformFacade;
import org.chovy.canvas.platform.domain.AdminPlatformCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理后台应用服务，负责把接口参数标准化后委托给管理目录。
 */
@Service
public class AdminPlatformApplicationService implements AdminPlatformFacade {

    /**
     * 保存管理后台用户、项目、系统选项和租户数据的目录。
     */
    private final AdminPlatformCatalog catalog;

    /**
     * 使用默认内存目录创建管理后台应用服务。
     */
    public AdminPlatformApplicationService() {
        this(new AdminPlatformCatalog());
    }

    /**
     * 使用指定目录创建管理后台应用服务。
     *
     * @param catalog 管理后台目录
     */
    public AdminPlatformApplicationService(AdminPlatformCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 查询租户下的后台用户。
     *
     * @param tenantId 租户标识
     * @return 用户记录列表
     */
    @Override
    public List<Map<String, Object>> users(Long tenantId) {
        return catalog.users(safeTenantId(tenantId));
    }

    /**
     * 创建后台用户。
     *
     * @param tenantId 租户标识
     * @param payload 用户创建参数
     * @param actor 操作者
     * @return 创建后的用户记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createUser(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createUser(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 更新后台用户。
     *
     * @param tenantId 租户标识
     * @param id 用户标识
     * @param payload 用户更新参数
     * @param actor 操作者
     * @return 更新后的用户记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateUser(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.updateUser(safeTenantId(tenantId), id, safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 禁用后台用户。
     *
     * @param tenantId 租户标识
     * @param id 用户标识
     * @param actor 操作者
     * @return 禁用后的用户记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> disableUser(Long tenantId, Long id, String actor) {
        return catalog.disableUser(safeTenantId(tenantId), id, actorOrDefault(actor));
    }

    /**
     * 查询租户下的项目。
     *
     * @param tenantId 租户标识
     * @return 项目记录列表
     */
    @Override
    public List<Map<String, Object>> projects(Long tenantId) {
        return catalog.projects(safeTenantId(tenantId));
    }

    /**
     * 创建项目。
     *
     * @param tenantId 租户标识
     * @param payload 项目创建参数
     * @param actor 操作者
     * @return 创建后的项目记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createProject(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.createProject(safeTenantId(tenantId), safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 查询单个项目。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @return 项目记录
     */
    @Override
    public Map<String, Object> project(Long tenantId, Long projectId) {
        return catalog.project(safeTenantId(tenantId), projectId);
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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateProject(Long tenantId, Long projectId, Map<String, Object> payload,
                                             String actor) {
        return catalog.updateProject(safeTenantId(tenantId), projectId, safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 禁用项目。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @param actor 操作者
     * @return 禁用后的项目记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> disableProject(Long tenantId, Long projectId, String actor) {
        return catalog.disableProject(safeTenantId(tenantId), projectId, actorOrDefault(actor));
    }

    /**
     * 查询项目成员。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @return 项目成员列表
     */
    @Override
    public List<Map<String, Object>> projectMembers(Long tenantId, Long projectId) {
        return catalog.projectMembers(safeTenantId(tenantId), projectId);
    }

    /**
     * 新增或更新项目成员。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @param userId 用户标识
     * @param payload 成员配置参数
     * @param actor 操作者
     * @return 成员记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> setProjectMember(Long tenantId, Long projectId, Long userId,
                                                Map<String, Object> payload, String actor) {
        return catalog.setProjectMember(safeTenantId(tenantId), projectId, userId, safePayload(payload),
                actorOrDefault(actor));
    }

    /**
     * 移除项目成员。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @param userId 用户标识
     * @return 移除结果记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> removeProjectMember(Long tenantId, Long projectId, Long userId) {
        return catalog.removeProjectMember(safeTenantId(tenantId), projectId, userId);
    }

    /**
     * 分页查询项目画布。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @param page 页码
     * @param size 每页数量
     * @return 画布分页结果
     */
    @Override
    public Map<String, Object> projectCanvases(Long tenantId, Long projectId, Integer page, Integer size) {
        // 未传分页参数时沿用后台默认首页，避免调用方必须显式传递分页值。
        return catalog.projectCanvases(safeTenantId(tenantId), projectId, page == null ? 1 : page,
                size == null ? 20 : size);
    }

    /**
     * 查询项目统计信息。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @return 项目统计结果
     */
    @Override
    public Map<String, Object> projectStats(Long tenantId, Long projectId) {
        return catalog.projectStats(safeTenantId(tenantId), projectId);
    }

    /**
     * 查询系统选项。
     *
     * @param tenantId 当前租户标识
     * @param category 选项分类
     * @param enabled 启用状态过滤值
     * @param keyword 关键字过滤值
     * @param requestedTenantId 请求指定的租户标识
     * @return 系统选项列表
     */
    @Override
    public List<Map<String, Object>> systemOptions(Long tenantId, String category, Integer enabled, String keyword,
                                                   Long requestedTenantId) {
        return catalog.systemOptions(safeTenantId(tenantId), category, enabled, keyword, requestedTenantId);
    }

    /**
     * 更新系统选项。
     *
     * @param tenantId 租户标识
     * @param id 选项标识
     * @param payload 选项更新参数
     * @param actor 操作者
     * @return 更新后的系统选项记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateSystemOption(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        return catalog.updateSystemOption(safeTenantId(tenantId), id, safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 查询全部租户。
     *
     * @return 租户记录列表
     */
    @Override
    public List<Map<String, Object>> tenants() {
        return catalog.tenants();
    }

    /**
     * 创建租户。
     *
     * @param payload 租户创建参数
     * @param actor 操作者
     * @return 创建后的租户记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createTenant(Map<String, Object> payload, String actor) {
        return catalog.createTenant(safePayload(payload), actorOrDefault(actor));
    }

    /**
     * 禁用租户。
     *
     * @param id 租户标识
     * @param actor 操作者
     * @return 禁用后的租户记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> disableTenant(Long id, String actor) {
        return catalog.disableTenant(id, actorOrDefault(actor));
    }

    /**
     * 激活租户。
     *
     * @param id 租户标识
     * @param actor 操作者
     * @return 激活后的租户记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> activateTenant(Long id, String actor) {
        return catalog.activateTenant(id, actorOrDefault(actor));
    }

    /**
     * 查询租户使用量。
     *
     * @param id 租户标识
     * @return 租户使用量记录
     */
    @Override
    public Map<String, Object> tenantUsage(Long id) {
        return catalog.tenantUsage(id);
    }

    /**
     * 将缺失或非法租户标识归一到默认租户。
     *
     * @param tenantId 原始租户标识
     * @return 可传递给目录层的租户标识
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 将空请求体归一为空 Map。
     *
     * @param payload 原始请求体
     * @return 非空请求体
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    /**
     * 将缺失操作者归一为系统操作者。
     *
     * @param actor 原始操作者
     * @return 可审计的操作者名称
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }
}
