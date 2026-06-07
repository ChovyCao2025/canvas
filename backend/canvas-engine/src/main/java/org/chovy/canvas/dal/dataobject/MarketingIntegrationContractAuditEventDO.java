package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_integration_contract_audit_event")
public class MarketingIntegrationContractAuditEventDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long contractId;

    private String contractKey;

    private Integer revision;

    private String eventType;

    private String previousStatus;

    private String newStatus;

    private String snapshotJson;

    private String changedFieldsJson;

    private String changedBy;

    private LocalDateTime createdAt;
}
