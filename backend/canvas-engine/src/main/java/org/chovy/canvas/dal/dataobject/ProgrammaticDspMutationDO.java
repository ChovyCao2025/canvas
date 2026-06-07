package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("programmatic_dsp_mutation")
public class ProgrammaticDspMutationDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long seatId;

    private Long campaignId;

    private Long lineItemId;

    private Long supplyPathId;

    private String provider;

    private String mutationKey;

    private String mutationType;

    private String entityType;

    private String externalEntityId;

    private String requestHash;

    private String idempotencyKey;

    private String status;

    private String approvalStatus;

    private Integer dryRunRequired;

    private String payloadJson;

    private String validationJson;

    private String providerRequestJson;

    private String providerResponseJson;

    private String errorCode;

    private String errorMessage;

    private String createdBy;

    private String approvedBy;

    private LocalDateTime approvedAt;

    private String executedBy;

    private LocalDateTime executedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
