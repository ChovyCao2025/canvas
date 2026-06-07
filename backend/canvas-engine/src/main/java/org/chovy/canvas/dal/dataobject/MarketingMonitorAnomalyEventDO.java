package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("marketing_monitor_anomaly_event")
public class MarketingMonitorAnomalyEventDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long ruleId;

    private String ruleKey;

    private Long sourceId;

    private String sourceKey;

    private String metricKey;

    private String bucketGrain;

    private LocalDateTime bucketStart;

    private LocalDateTime bucketEnd;

    private String brandKey;

    private String competitorKey;

    private BigDecimal actualValue;

    private BigDecimal baselineMedian;

    private BigDecimal baselineMad;

    private BigDecimal robustZScore;

    private BigDecimal deltaValue;

    private String direction;

    private String severity;

    private String status;

    private String evidenceJson;

    private String createdBy;

    private String resolvedBy;

    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
