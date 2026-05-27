package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户标签 数据对象，对应数据库表 {@code customer_tag}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("customer_tag")
public class CustomerTagDO {
    @TableId(type = IdType.AUTO)
    /** 客户标签记录主键 ID */
    private Long id;

    /** 业务用户 ID */
    private String userId;

    /** 标签名称或标签编码 */
    private String tag;

    /** 标签来源，如 CANVAS、IMPORT、MANUAL */
    private String source;

    /** 标签过期时间，null 表示长期有效 */
    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
