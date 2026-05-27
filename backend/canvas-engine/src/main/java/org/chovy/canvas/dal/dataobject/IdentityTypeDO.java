package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 身份类型 数据对象，对应数据库表 {@code identity_type}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("identity_type")
public class IdentityTypeDO {

    @TableId(type = IdType.AUTO)
    /** 身份类型主键 ID */
    private Long id;

    /** 身份类型编码，如手机号、邮箱、会员号等 */
    private String code;

    /** 身份类型展示名称 */
    private String name;

    /** 身份类型说明 */
    private String description;

    /** 是否启用，1=启用，0=禁用 */
    private Integer enabled;

    /** 是否允许通过导入任务写入该身份类型，1=允许，0=不允许 */
    private Integer allowImport;

    /** 同一用户是否允许存在多个该类型身份值，1=允许，0=不允许 */
    private Integer multiValue;

    /** 身份匹配优先级，数值越小优先级越高 */
    private Integer priority;

    /** 是否参与用户身份映射，1=参与，0=仅做展示或扩展字段 */
    private Integer participateMapping;

    /** 创建人 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
