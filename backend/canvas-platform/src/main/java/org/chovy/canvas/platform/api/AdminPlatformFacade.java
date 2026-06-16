package org.chovy.canvas.platform.api;

import java.util.List;
import java.util.Map;

/**
 * 提供管理后台用户、项目、系统选项和租户治理能力的应用入口。
 */
public interface AdminPlatformFacade {

    /**
     * 查询指定租户下的后台用户。
     *
     * @param tenantId 租户标识
     * @return 用户记录列表
     */
    List<Map<String, Object>> users(Long tenantId);

    /**
     * 创建后台用户。
     *
     * @param tenantId 租户标识
     * @param payload 用户创建参数
     * @param actor 操作者
     * @return 创建后的用户记录
     */
    Map<String, Object> createUser(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 更新后台用户。
     *
     * @param tenantId 租户标识
     * @param id 用户标识
     * @param payload 用户更新参数
     * @param actor 操作者
     * @return 更新后的用户记录
     */
    Map<String, Object> updateUser(Long tenantId, Long id, Map<String, Object> payload, String actor);

    /**
     * 禁用后台用户。
     *
     * @param tenantId 租户标识
     * @param id 用户标识
     * @param actor 操作者
     * @return 禁用后的用户记录
     */
    Map<String, Object> disableUser(Long tenantId, Long id, String actor);

    /**
     * 查询指定租户下的项目。
     *
     * @param tenantId 租户标识
     * @return 项目记录列表
     */
    List<Map<String, Object>> projects(Long tenantId);

    /**
     * 创建项目。
     *
     * @param tenantId 租户标识
     * @param payload 项目创建参数
     * @param actor 操作者
     * @return 创建后的项目记录
     */
    Map<String, Object> createProject(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 查询单个项目详情。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @return 项目记录
     */
    Map<String, Object> project(Long tenantId, Long projectId);

    /**
     * 更新项目详情。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @param payload 项目更新参数
     * @param actor 操作者
     * @return 更新后的项目记录
     */
    Map<String, Object> updateProject(Long tenantId, Long projectId, Map<String, Object> payload, String actor);

    /**
     * 禁用项目。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @param actor 操作者
     * @return 禁用后的项目记录
     */
    Map<String, Object> disableProject(Long tenantId, Long projectId, String actor);

    /**
     * 查询项目成员。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @return 项目成员列表
     */
    List<Map<String, Object>> projectMembers(Long tenantId, Long projectId);

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
    Map<String, Object> setProjectMember(Long tenantId, Long projectId, Long userId, Map<String, Object> payload,
                                         String actor);

    /**
     * 移除项目成员。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @param userId 用户标识
     * @return 移除结果记录
     */
    Map<String, Object> removeProjectMember(Long tenantId, Long projectId, Long userId);

    /**
     * 分页查询项目下的画布。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @param page 页码
     * @param size 每页数量
     * @return 画布分页结果
     */
    Map<String, Object> projectCanvases(Long tenantId, Long projectId, Integer page, Integer size);

    /**
     * 查询项目统计信息。
     *
     * @param tenantId 租户标识
     * @param projectId 项目标识
     * @return 项目统计结果
     */
    Map<String, Object> projectStats(Long tenantId, Long projectId);

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
    List<Map<String, Object>> systemOptions(Long tenantId, String category, Integer enabled, String keyword,
                                            Long requestedTenantId);

    /**
     * 更新系统选项。
     *
     * @param tenantId 租户标识
     * @param id 选项标识
     * @param payload 选项更新参数
     * @param actor 操作者
     * @return 更新后的系统选项记录
     */
    Map<String, Object> updateSystemOption(Long tenantId, Long id, Map<String, Object> payload, String actor);

    /**
     * 查询全部租户。
     *
     * @return 租户记录列表
     */
    List<Map<String, Object>> tenants();

    /**
     * 创建租户。
     *
     * @param payload 租户创建参数
     * @param actor 操作者
     * @return 创建后的租户记录
     */
    Map<String, Object> createTenant(Map<String, Object> payload, String actor);

    /**
     * 禁用租户。
     *
     * @param id 租户标识
     * @param actor 操作者
     * @return 禁用后的租户记录
     */
    Map<String, Object> disableTenant(Long id, String actor);

    /**
     * 激活租户。
     *
     * @param id 租户标识
     * @param actor 操作者
     * @return 激活后的租户记录
     */
    Map<String, Object> activateTenant(Long id, String actor);

    /**
     * 查询租户使用量摘要。
     *
     * @param id 租户标识
     * @return 租户使用量记录
     */
    Map<String, Object> tenantUsage(Long id);
}
