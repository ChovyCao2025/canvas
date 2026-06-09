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
 * ApprovalDefinitionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("approval_definition")
public class ApprovalDefinitionDO {

    /** 审批定义主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;
    /** 审批定义定义业务键 */
    private String definitionKey;
    /** 审批定义名称 */
    private String name;
    /** 审批定义领域 */
    private String domain;
    /** 审批定义目标类型 */
    private String targetType;
    /** 审批定义是否启用 */
    private Integer enabled;
    /** 审批定义模式 */
    private String mode;
    /** 审批定义最小审批数 */
    private Integer minApprovals;
    /** 审批定义默认截止小时 */
    private Integer defaultDueHours;
    /** 审批定义外部服务商 */
    private String externalProvider;
    /** 审批定义外部定义编码 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String externalDefinitionCode;
    /** 审批定义风险规则明细 JSON */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String riskRuleJson;
    /** 审批定义创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 审批定义最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
