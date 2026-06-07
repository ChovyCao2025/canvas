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
@TableName("approval_audit_event")
public class ApprovalAuditEventDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long instanceId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long taskId;
    private String eventType;
    private String actor;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String actorRole;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String oldStatus;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String newStatus;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String detailJson;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
