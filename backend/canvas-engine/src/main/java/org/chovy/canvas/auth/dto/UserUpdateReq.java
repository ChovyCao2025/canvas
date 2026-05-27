package org.chovy.canvas.auth.dto;

import lombok.Data;

/**
 * 更新用户请求（管理员接口）。
 *
 * <p>字段均为可选，仅更新传入字段。
 * 未传字段保持原值不变。
 *
 * 调用约定：
 * - `null` 表示“不更新该字段”；
 * - `password` 为空串会被服务层忽略，不会把密码改成空值。
 */
@Data
public class UserUpdateReq {

    /** 展示名（可选，传空字符串时由服务层决定是否视为有效更新）。 */
    private String displayName;

    /** 新密码（可选，服务层会做非空判断后再加密）。 */
    private String password;

    /** 角色（可选）：SUPER_ADMIN / TENANT_ADMIN / OPERATOR。 */
    private String role;
}
