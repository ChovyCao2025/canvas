package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_task_record")
public class CustomerTaskRecordDO {
    public static final String STATUS_OPEN = "OPEN";

    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String taskType;
    private String title;
    private String description;
    private String priority;
    private String assignee;
    private LocalDateTime dueAt;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
