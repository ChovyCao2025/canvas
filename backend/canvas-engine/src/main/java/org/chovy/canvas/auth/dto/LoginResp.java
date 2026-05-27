package org.chovy.canvas.auth.dto;

import lombok.Data;

/**
 * 登录响应参数。
 *
 * <p>该对象会被前端 AuthContext 持久化到 localStorage，
 * 用于后续路由鉴权和角色判断。
 * 不包含 password、enabled 等敏感或内部控制字段。
 */
@Data
public class LoginResp {

    /** JWT 访问令牌。 */
    private String token;

    /** 用户 ID。 */
    private Long userId;

    /** Tenant ID. */
    private Long tenantId;

    /** 用户名（唯一标识）。 */
    private String username;

    /** 展示名（用于侧边栏头像区和用户菜单显示）。 */
    private String displayName;

    /** 用户角色（ADMIN / SUPER_ADMIN / TENANT_ADMIN / OPERATOR）。 */
    private String role;
}
