package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户画像 数据对象，对应数据库表 {@code customer_profile}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("customer_profile")
public class CustomerProfileDO {
    @TableId(type = IdType.AUTO)
    /** 客户画像主键 ID */
    private Long id;

    /** 业务用户 ID */
    private String userId;

    /** 用户所在时区，用于静默时段和本地时间判断 */
    private String timezone;

    /** 用户所属地区或区域编码 */
    private String region;

    /** 用户生命周期阶段，如 NEW、ACTIVE、CHURN_RISK */
    private String lifecycleStage;

    /** 扩展画像属性 JSON */
    private String attributes;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
