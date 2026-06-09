package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MarketingMonitorTrendSnapshotDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_monitor_trend_snapshot")
public class MarketingMonitorTrendSnapshotDO {

    /** 营销监控趋势SNAPSHOT主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 营销监控趋势SNAPSHOT来源 ID */
    private Long sourceId;

    /** 营销监控趋势SNAPSHOTSOURCEKEY业务键 */
    private String sourceKey;

    /** 营销监控趋势SNAPSHOTBUCKETGRAIN */
    private String bucketGrain;

    /** 营销监控趋势SNAPSHOTBUCKETSTART */
    private LocalDateTime bucketStart;

    /** 营销监控趋势SNAPSHOTBUCKETEND */
    private LocalDateTime bucketEnd;

    /** 营销监控趋势SNAPSHOTBRANDKEY业务键 */
    private String brandKey;

    /** 营销监控趋势SNAPSHOTCOMPETITORKEY业务键 */
    private String competitorKey;

    /** 营销监控趋势SNAPSHOTMENTIONCOUNT数量 */
    private Integer mentionCount;

    /** 营销监控趋势SNAPSHOTPOSITIVECOUNT数量 */
    private Integer positiveCount;

    /** 营销监控趋势SNAPSHOTNEUTRALCOUNT数量 */
    private Integer neutralCount;

    /** 营销监控趋势SNAPSHOTNEGATIVECOUNT数量 */
    private Integer negativeCount;

    /** 营销监控趋势SNAPSHOTCOMPETITORCOUNT数量 */
    private Integer competitorCount;

    /** 营销监控趋势SNAPSHOTALERTCOUNT数量 */
    private Integer alertCount;

    /** 营销监控趋势SNAPSHOTAVG情绪SCORE */
    private BigDecimal avgSentimentScore;

    /** 营销监控趋势SNAPSHOTMETADATAJSON明细 JSON */
    private String metadataJson;

    /** 营销监控趋势SNAPSHOT创建人 */
    private String createdBy;

    /** 营销监控趋势SNAPSHOT创建时间 */
    private LocalDateTime createdAt;

    /** 营销监控趋势SNAPSHOT最后更新时间 */
    private LocalDateTime updatedAt;
}
