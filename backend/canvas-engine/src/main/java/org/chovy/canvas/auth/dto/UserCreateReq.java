package org.chovy.canvas.auth.dto;

import lombok.Data;

/**
 * 创建用户请求（管理员接口）。
 *
 * <p>用于后台用户管理页“新建用户”弹窗提交。
 * 该请求会落到 `SysUserService#create`，由服务层完成密码加密与入库。
 */
@Data
public class UserCreateReq {

    /** 登录用户名（唯一，建议限制字符集并去除首尾空格）。 */
    private String username;

    /** 初始密码（后端会做 BCrypt 加密后存储）。 */
    private String password;

    /** 展示名（用于后台列表和操作审计展示）。 */
    private String displayName;

    /** 角色：`ADMIN` / `OPERATOR`。 */
    private String role;
}
