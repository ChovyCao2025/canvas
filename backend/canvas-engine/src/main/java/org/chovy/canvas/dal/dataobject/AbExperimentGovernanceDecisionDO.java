package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ab_experiment_governance_decision")
public class AbExperimentGovernanceDecisionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long experimentId;

    private String status;

    private String winnerVariantKey;

    private String primaryMetricKey;

    private BigDecimal confidence;

    private Long requiredSampleSize;

    private String reason;

    private String writebackStatus;

    private LocalDateTime evaluatedAt;

    private LocalDateTime createdAt;
}
