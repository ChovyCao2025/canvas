package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异步任务 Subscription 数据对象，对应数据库表 {@code async_task_subscription}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("async_task_subscription")
public class AsyncTaskSubscriptionDO {

    @TableId(type = IdType.AUTO)
    /** 任务订阅记录主键 ID */
    private Long id;

    /** 被订阅的异步任务 ID，对应 async_task.task_id */
    private String taskId;

    /** 订阅该任务通知的用户 ID */
    private String userId;

    @TableField(fill = FieldFill.INSERT)
    /** 订阅创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;
}
