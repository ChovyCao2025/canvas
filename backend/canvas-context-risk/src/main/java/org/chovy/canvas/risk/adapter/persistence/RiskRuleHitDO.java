package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控规则命中实体，保存某次决策中命中的规则、动作、分数和证据。
 */
@Data
@TableName("risk_rule_hit")
public class RiskRuleHitDO {

    /**
     * 规则命中记录的自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则命中所属租户。
     */
    private Long tenantId;
    /**
     * 产生该命中的风控决策执行记录 ID。
     */
    private Long decisionRunId;
    /**
     * 命中时使用的策略键。
     */
    private String strategyKey;
    /**
     * 命中时使用的策略版本号。
     */
    private Integer strategyVersion;
    /**
     * 命中规则所在的规则组键。
     */
    private String groupKey;
    /**
     * 规则组内的规则键。
     */
    private String ruleKey;
    /**
     * 规则评估时的运行模式。
     */
    private String mode;
    /**
     * 命中规则贡献的决策动作。
     */
    private String action;
    /**
     * 命中规则贡献的风险分增量。
     */
    private Integer scoreDelta;
    /**
     * 命中规则输出的原因码。
     */
    private String reasonCode;
    /**
     * 序列化后的命中证据，用于追踪和调查页面。
     */
    private String evidenceJson;
    /**
     * 规则命中记录创建时间。
     */
    private LocalDateTime createdAt;
}
