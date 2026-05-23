package org.chovy.canvas.domain.task;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("async_task_subscription")
public class AsyncTaskSubscription {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private String userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
