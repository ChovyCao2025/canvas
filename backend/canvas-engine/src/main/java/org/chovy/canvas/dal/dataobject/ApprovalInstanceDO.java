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
 * ApprovalInstanceDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("approval_instance")
public class ApprovalInstanceDO {

    /** 审批实例主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;
    /** 审批实例定义业务键 */
    private String definitionKey;
    /** 审批实例领域 */
    private String domain;
    /** 审批实例目标类型 */
    private String targetType;
    /** 关联的目标 ID */
    private String targetId;
    /** 关联的目标版本 ID */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long targetVersionId;
    /** 审批实例当前状态 */
    private String status;
    /** 审批实例提交人 */
    private String submitter;
    /** 审批实例提交原因 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String submitReason;
    /** 审批实例风险级别 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String riskLevel;
    /** 审批实例风险原因 JSON */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String riskReasonsJson;
    /** 审批实例快照明细 JSON */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String snapshotJson;
    /** 关联的外部实例 ID */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String externalInstanceId;
    /** 审批实例请求时间 */
    private LocalDateTime requestedAt;
    /** 审批实例完成时间 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime completedAt;
    /** 审批实例完成人 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String completedBy;
    /** 审批实例结果备注 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String resultComment;
    /** 审批实例自动动作 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String autoAction;
    /** 审批实例自动动作状态 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String autoActionStatus;
    /** 审批实例自动动作错误 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String autoActionError;
    /** 审批实例创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 审批实例最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
