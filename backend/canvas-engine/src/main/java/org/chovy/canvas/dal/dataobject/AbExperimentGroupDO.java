package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AB 实验 Group 数据对象，对应数据库表 {@code ab_experiment_group}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("ab_experiment_group")
public class AbExperimentGroupDO {

    @TableId(type = IdType.AUTO)
    /** 实验分组主键 ID */
    private Long id;

    /** 所属 AB 实验 ID，对应 ab_experiment.id */
    private Long experimentId;

    /** 实验平台返回的分组标识，用于匹配 SPLIT 节点出口 */
    private String groupKey;

    /** 分组展示名称，如 A 组、B 组或对照组 */
    private String label;

    /** 分组展示排序，值越小越靠前 */
    private Integer sortOrder;

    /** 是否启用该分组，1=启用，0=禁用 */
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
