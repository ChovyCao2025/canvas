package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AbExperimentGovernanceDecisionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("ab_experiment_governance_decision")
public class AbExperimentGovernanceDecisionDO {

    /** A/B实验治理决策主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的实验 ID */
    private Long experimentId;

    /** A/B实验治理决策当前状态 */
    private String status;

    /** A/B实验治理决策获胜变体业务键 */
    private String winnerVariantKey;

    /** A/B实验治理决策主指标业务键 */
    private String primaryMetricKey;

    /** A/B实验治理决策置信度 */
    private BigDecimal confidence;

    /** A/B实验治理决策要求样本大小 */
    private Long requiredSampleSize;

    /** A/B实验治理决策原因说明 */
    private String reason;

    /** A/B实验治理决策回写状态 */
    private String writebackStatus;

    /** A/B实验治理决策评估时间 */
    private LocalDateTime evaluatedAt;

    /** A/B实验治理决策创建时间 */
    private LocalDateTime createdAt;
}
