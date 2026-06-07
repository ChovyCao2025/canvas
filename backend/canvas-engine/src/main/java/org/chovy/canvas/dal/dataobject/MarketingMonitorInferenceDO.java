package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("marketing_monitor_inference")
public class MarketingMonitorInferenceDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long itemId;

    private Long sourceId;

    private Long providerId;

    private Long templateId;

    private String modelKey;

    private String modelVersion;

    private String providerStatus;

    private Boolean fallbackUsed;

    private String inputHash;

    private String promptHash;

    private String sentimentLabel;

    private BigDecimal sentimentScore;

    private BigDecimal confidence;

    private String entitiesJson;

    private String topicsJson;

    private String riskFlagsJson;

    private String evidenceJson;

    private Long latencyMs;

    private String requestedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
