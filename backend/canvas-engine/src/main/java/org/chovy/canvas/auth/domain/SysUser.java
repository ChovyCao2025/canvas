package org.chovy.canvas.auth.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统用户（sys_user）。
 *
 * 该实体用于认证与后台用户管理，不承载业务域数据。
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 主键 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录用户名（唯一）。 */
    private String username;

    /** BCrypt 加密密码，查询时不返回给前端 */
    @TableField(select = false)
    private String password;

    /** 展示名。 */
    private String displayName;

    /** ADMIN / OPERATOR */
    private String role;

    /** 启用状态：1=启用，0=禁用。 */
    private Integer enabled;

    /** 创建时间（插入时自动填充）。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（插入/更新时自动填充）。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
