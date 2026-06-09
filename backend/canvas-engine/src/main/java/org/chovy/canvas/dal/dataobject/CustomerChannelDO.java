package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户渠道 数据对象，对应数据库表 {@code customer_channel}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("customer_channel")
public class CustomerChannelDO {
    @TableId(type = IdType.AUTO)
    /** 客户渠道记录主键 ID */
    private Long id;

    /** 所属租户 ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 业务用户 ID */
    private String userId;

    /** 渠道类型，如 SMS、EMAIL、PUSH、WECHAT */
    private String channel;

    /** 渠道地址，如手机号、邮箱或设备标识 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String address;

    /** 是否启用该渠道，1=启用，0=禁用 */
    private Integer enabled;

    /** 渠道地址是否已验证，1=已验证，0=未验证 */
    private Integer verified;

    /** 渠道扩展信息 JSON，如设备、地区或第三方账号信息 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String metadata;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
