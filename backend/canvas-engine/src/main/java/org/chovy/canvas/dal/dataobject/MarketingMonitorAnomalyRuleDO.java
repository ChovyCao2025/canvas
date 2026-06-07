package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("marketing_monitor_anomaly_rule")
public class MarketingMonitorAnomalyRuleDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String ruleKey;

    private String displayName;

    private Long sourceId;

    private String metricKey;

    private String bucketGrain;

    private String brandKey;

    private String competitorKey;

    private String direction;

    private Integer baselineWindowBuckets;

    private Integer minBaselineBuckets;

    private BigDecimal thresholdMultiplier;

    private BigDecimal minDelta;

    private Integer enabled;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
