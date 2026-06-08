package org.chovy.canvas.domain.bi.resource;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.stereotype.Service;

/**
 * BiResourcePermissionGuard 编排 domain.bi.resource 场景的领域业务规则。
 */
@Service
public class BiResourcePermissionGuard {

    private final BiPermissionService permissionService;

    /**
     * 创建 BiResourcePermissionGuard 实例并注入 domain.bi.resource 场景依赖。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiResourcePermissionGuard(BiPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 对 BI 资源执行统一的动作级权限校验。
     *
     * <p>资源 ID 为空时代表调用方还处于创建前流程，方法直接放行；已有资源会使用租户、工作区、资源类型、
     * 资源 ID、用户名和角色构造查询上下文，再委托权限服务校验 actionKey。缺失用户或角色会降级为 system/operator，
     * 避免后台任务因上下文为空绕过权限模型。</p>
     *
     * @param tenantId 租户 ID，空值按系统租户 0 处理
     * @param workspaceId 工作区 ID，用于限定资源权限范围
     * @param resourceType 资源类型，例如 dataset、chart、dashboard
     * @param resourceId 资源 ID；为空时跳过校验
     * @param username 操作人账号，缺失时使用 system
     * @param role 操作人角色，缺失时使用 operator
     * @param actionKey 权限动作 key，例如 read、edit、publish
     */
    public void require(Long tenantId,
                        Long workspaceId,
                        String resourceType,
                        Long resourceId,
                        String username,
                        String role,
                        String actionKey) {
        if (resourceId == null) {
            return;
        }
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        permissionService.enforceResourceAccess(
                scopedTenantId,
                workspaceId,
                resourceType,
                resourceId,
                new BiQueryContext(scopedTenantId, defaultUser(username), defaultRole(role)),
                actionKey);
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 default user 生成的文本或业务键。
     */
    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @return 返回 default role 生成的文本或业务键。
     */
    private String defaultRole(String role) {
        return role == null || role.isBlank() ? RoleNames.OPERATOR : role;
    }
}
