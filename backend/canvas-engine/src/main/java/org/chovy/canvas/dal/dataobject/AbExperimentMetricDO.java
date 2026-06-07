package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ab_experiment_metric")
public class AbExperimentMetricDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long experimentId;

    private String metricKey;

    private String displayName;

    private String metricRole;

    private String direction;

    private BigDecimal minimumDetectableEffect;

    private BigDecimal guardrailMaxRegression;

    private Integer minimumSampleSize;

    private Integer enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
