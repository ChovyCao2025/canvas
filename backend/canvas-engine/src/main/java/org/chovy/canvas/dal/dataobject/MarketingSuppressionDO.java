package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 营销抑制 数据对象，对应数据库表 {@code marketing_suppression}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("marketing_suppression")
public class MarketingSuppressionDO {
    @TableId(type = IdType.AUTO)
    /** 营销抑制记录主键 ID */
    private Long id;

    /** 业务用户 ID */
    private String userId;

    /** 被抑制的渠道，如 SMS、EMAIL、PUSH、WECHAT */
    private String channel;

    /** 抑制原因，如退订、投诉、风控或静默策略 */
    private String reason;

    /** 是否生效，1=生效，0=失效 */
    private Integer active;

    /** 抑制过期时间，null 表示长期抑制 */
    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
