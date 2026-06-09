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
 * ApprovalAuditEventDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("approval_audit_event")
public class ApprovalAuditEventDO {

    /** 审批审计事件主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的实例 ID */
    private Long instanceId;
    /** 关联的任务 ID */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long taskId;
    /** 审批审计事件事件类型 */
    private String eventType;
    /** 审批审计事件操作人 */
    private String actor;
    /** 审批审计事件操作人角色 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String actorRole;
    /** 审批审计事件原状态 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String oldStatus;
    /** 审批审计事件新状态 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String newStatus;
    /** 审批审计事件明细 JSON */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String detailJson;
    /** 审批审计事件创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
