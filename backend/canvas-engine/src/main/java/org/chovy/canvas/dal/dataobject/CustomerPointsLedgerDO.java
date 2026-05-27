package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户积分流水 数据对象，对应数据库表 {@code customer_points_ledger}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("customer_points_ledger")
public class CustomerPointsLedgerDO {
    @TableId(type = IdType.AUTO)
    /** 积分流水主键 ID */
    private Long id;

    /** 业务用户 ID */
    private String userId;

    /** 积分操作类型，如 ADD、DEDUCT、EXPIRE */
    private String operation;

    /** 本次变动积分数量，正负语义由 operation 决定 */
    private Integer points;

    /** 积分类型，如普通积分、成长值或活动积分 */
    private String pointsType;

    /** 积分变动原因 */
    private String reason;

    /** 幂等键，用于防止重复记账 */
    private String idempotencyKey;

    /** 积分过期时间，null 表示不过期 */
    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    /** 流水创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;
}
