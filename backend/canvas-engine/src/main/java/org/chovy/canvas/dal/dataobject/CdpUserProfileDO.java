package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CDP 用户画像 数据对象，对应数据库表 {@code cdp_user_profile}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("cdp_user_profile")
public class CdpUserProfileDO {

    @TableId(type = IdType.AUTO)
    /** 用户画像记录主键 ID */
    private Long id;

    /** CDP 内部统一用户 ID */
    private String userId;

    /** 用户展示名称 */
    private String displayName;

    /** 用户手机号，敏感展示时需要脱敏 */
    private String phone;

    /** 用户邮箱，敏感展示时需要脱敏 */
    private String email;

    /** 用户状态，如 ACTIVE、DISABLED */
    private String status;

    /** 扩展画像属性 JSON */
    private String propertiesJson;

    /** 首次识别到该用户的时间 */
    private LocalDateTime firstSeenAt;

    /** 最近一次识别或更新该用户的时间 */
    private LocalDateTime lastSeenAt;

    /** 创建人或创建来源 */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
