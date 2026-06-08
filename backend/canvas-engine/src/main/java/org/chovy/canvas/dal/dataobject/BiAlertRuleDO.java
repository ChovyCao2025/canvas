package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiAlertRuleDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_alert_rule")
public class BiAlertRuleDO {

    /** BI告警规则主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作空间 ID */
    private Long workspaceId;

    /** BI告警规则告警业务键 */
    private String alertKey;

    /** BI告警规则名称 */
    private String name;

    /** 关联的数据集 ID */
    private Long datasetId;

    /** BI告警规则指标标识 */
    private String metricKey;

    /** BI告警规则触发条件 JSON */
    private String conditionJson;

    /** BI告警规则接收方配置 JSON */
    private String receiverJson;

    /** BI告警规则是否启用 */
    private Boolean enabled;

    /** BI告警规则创建人 */
    private String createdBy;

    /** BI告警规则创建时间 */
    private LocalDateTime createdAt;

    /** BI告警规则最后更新时间 */
    private LocalDateTime updatedAt;
}
