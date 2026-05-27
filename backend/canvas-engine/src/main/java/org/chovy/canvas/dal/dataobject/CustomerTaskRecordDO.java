package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户任务记录 数据对象，对应数据库表 {@code customer_task_record}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("customer_task_record")
public class CustomerTaskRecordDO {
    /** 任务未完成状态常量 */
    public static final String STATUS_OPEN = "OPEN";

    @TableId(type = IdType.AUTO)
    /** 客户任务记录主键 ID */
    private Long id;

    /** 业务用户 ID */
    private String userId;

    /** 任务类型，如回访、跟进、人工处理 */
    private String taskType;

    /** 任务标题 */
    private String title;

    /** 任务详细描述 */
    private String description;

    /** 任务优先级，如 LOW、MEDIUM、HIGH */
    private String priority;

    /** 任务负责人或处理人 */
    private String assignee;

    /** 任务截止时间 */
    private LocalDateTime dueAt;

    /** 任务状态，如 OPEN、DONE、CANCELED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
