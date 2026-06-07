package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_enterprise_olap_evidence_collection_run")
public class CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String triggerType;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Integer evidenceCount;

    private Integer passCount;

    private Integer warnCount;

    private Integer failCount;

    private String reason;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
