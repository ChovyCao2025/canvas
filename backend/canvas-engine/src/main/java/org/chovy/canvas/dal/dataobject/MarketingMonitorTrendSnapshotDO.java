package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("marketing_monitor_trend_snapshot")
public class MarketingMonitorTrendSnapshotDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long sourceId;

    private String sourceKey;

    private String bucketGrain;

    private LocalDateTime bucketStart;

    private LocalDateTime bucketEnd;

    private String brandKey;

    private String competitorKey;

    private Integer mentionCount;

    private Integer positiveCount;

    private Integer neutralCount;

    private Integer negativeCount;

    private Integer competitorCount;

    private Integer alertCount;

    private BigDecimal avgSentimentScore;

    private String metadataJson;

    private String createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
