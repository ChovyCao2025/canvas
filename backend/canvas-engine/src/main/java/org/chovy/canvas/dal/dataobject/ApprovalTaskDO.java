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
@TableName("approval_task")
public class ApprovalTaskDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long instanceId;
    private Integer stepNo;
    private String approver;
    private String status;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime dueAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime actedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String actionComment;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String delegatedFrom;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String externalTaskId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
