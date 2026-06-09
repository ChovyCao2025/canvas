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
 * ApprovalTaskDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("approval_task")
public class ApprovalTaskDO {

    /** 审批任务主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的实例 ID */
    private Long instanceId;
    /** 审批任务审批步骤序号 */
    private Integer stepNo;
    /** 审批任务审批人 */
    private String approver;
    /** 审批任务当前状态 */
    private String status;
    /** 审批任务截止时间 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime dueAt;
    /** 审批任务审批操作时间 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime actedAt;
    /** 审批任务动作备注 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String actionComment;
    /** 审批任务委派来源审批人 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String delegatedFrom;
    /** 关联的外部任务 ID */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String externalTaskId;
    /** 审批任务创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 审批任务最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
