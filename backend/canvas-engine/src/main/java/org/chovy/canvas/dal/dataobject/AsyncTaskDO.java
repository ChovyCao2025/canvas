package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("async_task")
public class AsyncTaskDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private String taskType;
    private String bizType;
    private String bizId;
    private String title;
    private String status;
    private Integer progress;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String resultSummary;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String errorMsg;
    private String createdBy;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime startedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime finishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
