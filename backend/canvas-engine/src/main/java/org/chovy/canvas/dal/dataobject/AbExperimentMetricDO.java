package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AbExperimentMetricDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("ab_experiment_metric")
public class AbExperimentMetricDO {

    /** A/B实验指标主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的实验 ID */
    private Long experimentId;

    /** A/B实验指标指标标识 */
    private String metricKey;

    /** A/B实验指标展示名称 */
    private String displayName;

    /** A/B实验指标指标角色 */
    private String metricRole;

    /** A/B实验指标指标方向 */
    private String direction;

    /** A/B实验指标最小可检测效果 */
    private BigDecimal minimumDetectableEffect;

    /** A/B实验指标护栏最大回退 */
    private BigDecimal guardrailMaxRegression;

    /** A/B实验指标最小样本大小 */
    private Integer minimumSampleSize;

    /** A/B实验指标是否启用 */
    private Integer enabled;

    /** A/B实验指标创建时间 */
    private LocalDateTime createdAt;

    /** A/B实验指标最后更新时间 */
    private LocalDateTime updatedAt;
}
