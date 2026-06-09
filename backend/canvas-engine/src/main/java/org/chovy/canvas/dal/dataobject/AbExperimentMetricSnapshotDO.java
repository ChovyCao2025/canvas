package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AbExperimentMetricSnapshotDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("ab_experiment_metric_snapshot")
public class AbExperimentMetricSnapshotDO {

    /** A/B实验指标快照主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的实验 ID */
    private Long experimentId;

    /** A/B实验指标快照实验变体标识 */
    private String variantKey;

    /** A/B实验指标快照指标标识 */
    private String metricKey;

    /** A/B实验指标快照样本量 */
    private Long sampleSize;

    /** A/B实验指标快照转化 */
    private Long conversions;

    /** A/B实验指标快照指标值 */
    private BigDecimal metricValue;

    /** A/B实验指标快照方差 */
    private BigDecimal variance;

    /** A/B实验指标快照观测时间 */
    private LocalDateTime observedAt;

    /** A/B实验指标快照来源 */
    private String source;

    /** A/B实验指标快照创建时间 */
    private LocalDateTime createdAt;
}
