package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ab_experiment_metric_snapshot")
public class AbExperimentMetricSnapshotDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long experimentId;

    private String variantKey;

    private String metricKey;

    private Long sampleSize;

    private Long conversions;

    private BigDecimal metricValue;

    private BigDecimal variance;

    private LocalDateTime observedAt;

    private String source;

    private LocalDateTime createdAt;
}
