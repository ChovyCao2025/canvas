package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingIntegrationContractAuditEventDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_integration_contract_audit_event")
public class MarketingIntegrationContractAuditEventDO {

    /** 营销集成契约审计事件主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的契约 ID */
    private Long contractId;

    /** 营销集成契约审计事件契约业务键 */
    private String contractKey;

    /** 营销集成契约审计事件修订 */
    private Integer revision;

    /** 营销集成契约审计事件事件类型 */
    private String eventType;

    /** 营销集成契约审计事件上一状态 */
    private String previousStatus;

    /** 营销集成契约审计事件新状态 */
    private String newStatus;

    /** 营销集成契约审计事件快照明细 JSON */
    private String snapshotJson;

    /** 营销集成契约审计事件变更字段明细 JSON */
    private String changedFieldsJson;

    /** 营销集成契约审计事件变更人 */
    private String changedBy;

    /** 营销集成契约审计事件创建时间 */
    private LocalDateTime createdAt;
}
