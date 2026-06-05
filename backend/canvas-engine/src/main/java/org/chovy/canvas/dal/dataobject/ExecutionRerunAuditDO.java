package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("execution_rerun_audit")
public class ExecutionRerunAuditDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long canvasId;
    private String userId;
    private Long testUserId;
    private String originalExecutionId;
    private String mode;
    private String reason;
    private String operator;
    private String status;
    private String inputParams;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
