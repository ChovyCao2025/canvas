package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CDP 用户身份 数据对象，对应数据库表 {@code cdp_user_identity}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("cdp_user_identity")
public class CdpUserIdentityDO {

    @TableId(type = IdType.AUTO)
    /** 用户身份记录主键 ID */
    private Long id;

    @TableField("tenant_id")
    /** 所属租户 ID */
    private Long tenantId;

    /** CDP 内部统一用户 ID */
    private String userId;

    /** 身份类型编码，如 phone、email、member_id */
    private String identityType;

    /** 身份值，如手机号、邮箱地址或会员号 */
    private String identityValue;

    /** 身份来源类型，如 CANVAS_EXECUTION、IMPORT、API */
    private String sourceType;

    /** 来源引用 ID，如执行 ID、导入批次 ID 或外部请求 ID */
    private String sourceRefId;

    /** 是否已验证，1=已验证，0=未验证 */
    private Integer verified;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
