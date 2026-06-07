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
@TableName("approval_definition")
public class ApprovalDefinitionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String definitionKey;
    private String name;
    private String domain;
    private String targetType;
    private Integer enabled;
    private String mode;
    private Integer minApprovals;
    private Integer defaultDueHours;
    private String externalProvider;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String externalDefinitionCode;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String riskRuleJson;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
