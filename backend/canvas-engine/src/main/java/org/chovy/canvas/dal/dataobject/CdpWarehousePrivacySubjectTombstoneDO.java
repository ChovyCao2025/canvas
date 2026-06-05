package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cdp_warehouse_privacy_subject_tombstone")
public class CdpWarehousePrivacySubjectTombstoneDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String subjectType;

    private String subjectHash;

    private String subjectRefMasked;

    private String status;

    private Long sourceRequestId;

    private String sourceRequestKey;

    private String reason;

    private Long blockedEventCount;

    private LocalDateTime lastBlockedAt;

    private String createdBy;

    private String revokedBy;

    private LocalDateTime revokedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
