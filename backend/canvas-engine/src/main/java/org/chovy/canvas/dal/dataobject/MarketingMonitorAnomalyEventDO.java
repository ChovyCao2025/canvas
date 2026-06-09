package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MarketingMonitorAnomalyEventDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_monitor_anomaly_event")
public class MarketingMonitorAnomalyEventDO {

    /** 营销监控异常事件主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的规则 ID */
    private Long ruleId;

    /** 营销监控异常事件规则业务键 */
    private String ruleKey;

    /** 营销监控异常事件来源 ID */
    private Long sourceId;

    /** 营销监控异常事件来源业务键 */
    private String sourceKey;

    /** 营销监控异常事件指标标识 */
    private String metricKey;

    /** 营销监控异常事件分桶粒度 */
    private String bucketGrain;

    /** 营销监控异常事件分桶起始值 */
    private LocalDateTime bucketStart;

    /** 营销监控异常事件分桶结束值 */
    private LocalDateTime bucketEnd;

    /** 营销监控异常事件品牌业务键 */
    private String brandKey;

    /** 营销监控异常事件竞品业务键 */
    private String competitorKey;

    /** 营销监控异常事件实际值 */
    private BigDecimal actualValue;

    /** 营销监控异常事件基线中位数 */
    private BigDecimal baselineMedian;

    /** 营销监控异常事件基线MAD */
    private BigDecimal baselineMad;

    /** 营销监控ANOMALYEVENTROBUSTZSCORE */
    private BigDecimal robustZScore;

    /** 营销监控异常事件增量值 */
    private BigDecimal deltaValue;

    /** 营销监控异常事件指标方向 */
    private String direction;

    /** 营销监控异常事件严重级别 */
    private String severity;

    /** 营销监控异常事件当前状态 */
    private String status;

    /** 营销监控异常事件证据明细 JSON */
    private String evidenceJson;

    /** 营销监控异常事件创建人 */
    private String createdBy;

    /** 营销监控异常事件解决人 */
    private String resolvedBy;

    /** 营销监控异常事件解决时间 */
    private LocalDateTime resolvedAt;

    /** 营销监控异常事件创建时间 */
    private LocalDateTime createdAt;

    /** 营销监控异常事件最后更新时间 */
    private LocalDateTime updatedAt;
}
