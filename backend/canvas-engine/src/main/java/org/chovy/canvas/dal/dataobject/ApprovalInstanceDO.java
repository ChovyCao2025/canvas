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
@TableName("approval_instance")
public class ApprovalInstanceDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String definitionKey;
    private String domain;
    private String targetType;
    private String targetId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long targetVersionId;
    private String status;
    private String submitter;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String submitReason;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String riskLevel;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String riskReasonsJson;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String snapshotJson;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String externalInstanceId;
    private LocalDateTime requestedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime completedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String completedBy;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String resultComment;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String autoAction;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String autoActionStatus;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String autoActionError;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
