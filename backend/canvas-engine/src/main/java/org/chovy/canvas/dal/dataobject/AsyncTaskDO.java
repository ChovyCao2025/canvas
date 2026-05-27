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
 * 异步任务 数据对象，对应数据库表 {@code async_task}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("async_task")
public class AsyncTaskDO {

    @TableId(type = IdType.AUTO)
    /** 异步任务表自增主键 ID */
    private Long id;

    /** 对外暴露的任务 ID，用于前端轮询和通知关联 */
    private String taskId;

    /** 任务类型，如 AUDIENCE_COMPUTE、TAG_IMPORT 等 */
    private String taskType;

    /** 业务类型，用于区分任务归属的业务域 */
    private String bizType;

    /** 业务对象 ID，如人群 ID、导入批次 ID 等 */
    private String bizId;

    /** 任务展示标题 */
    private String title;

    /** 任务状态，见 {@link org.chovy.canvas.domain.task.AsyncTaskStatus} */
    private String status;

    /** 任务进度百分比，取值范围 0~100 */
    private Integer progress;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    /** 任务结果摘要，允许更新为 null 清空 */
    private String resultSummary;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    /** 任务失败原因，允许更新为 null 清空 */
    private String errorMsg;

    /** 任务创建人或触发用户 */
    private String createdBy;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    /** 任务开始执行时间，允许更新为 null */
    private LocalDateTime startedAt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    /** 任务结束时间，成功、失败或取消时写入，允许更新为 null */
    private LocalDateTime finishedAt;

    @TableField(fill = FieldFill.INSERT)
    /** 记录创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 记录最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
