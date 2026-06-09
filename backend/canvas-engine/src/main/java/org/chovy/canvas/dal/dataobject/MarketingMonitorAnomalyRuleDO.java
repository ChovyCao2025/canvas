package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MarketingMonitorAnomalyRuleDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_monitor_anomaly_rule")
public class MarketingMonitorAnomalyRuleDO {

    /** 营销监控异常规则主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 营销监控异常规则规则业务键 */
    private String ruleKey;

    /** 营销监控异常规则展示名称 */
    private String displayName;

    /** 营销监控异常规则来源 ID */
    private Long sourceId;

    /** 营销监控异常规则指标标识 */
    private String metricKey;

    /** 营销监控异常规则分桶粒度 */
    private String bucketGrain;

    /** 营销监控异常规则品牌业务键 */
    private String brandKey;

    /** 营销监控异常规则竞品业务键 */
    private String competitorKey;

    /** 营销监控异常规则指标方向 */
    private String direction;

    /** 营销监控异常规则基线窗口分桶 */
    private Integer baselineWindowBuckets;

    /** 营销监控异常规则最小基线分桶 */
    private Integer minBaselineBuckets;

    /** 营销监控异常规则阈值倍数 */
    private BigDecimal thresholdMultiplier;

    /** 营销监控异常规则最小增量 */
    private BigDecimal minDelta;

    /** 营销监控异常规则是否启用 */
    private Integer enabled;

    /** 营销监控异常规则扩展元数据 JSON */
    private String metadataJson;

    /** 营销监控异常规则创建人 */
    private String createdBy;

    /** 营销监控异常规则创建时间 */
    private LocalDateTime createdAt;

    /** 营销监控异常规则最后更新时间 */
    private LocalDateTime updatedAt;
}
