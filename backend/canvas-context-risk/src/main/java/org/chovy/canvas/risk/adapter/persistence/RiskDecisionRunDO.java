package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控决策执行记录实体，保存一次决策的输入快照、输出结果和可重放审计信息。
 */
@Data
@TableName("risk_decision_run")
public class RiskDecisionRunDO {

    /**
     * 决策执行记录的自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 决策请求所属租户，用于隔离审计和回放数据。
     */
    private Long tenantId;
    /**
     * 调用方传入的幂等键，同一请求重复提交时用于查找既有结果。
     */
    private String requestId;
    /**
     * 标准化请求载荷的哈希值，用于拒绝 requestId 相同但事实数据不同的回放。
     */
    private String requestHash;
    /**
     * 本次决策评估的风控场景键。
     */
    private String sceneKey;
    /**
     * 评估时命中的策略键。
     */
    private String strategyKey;
    /**
     * 产生该决策的策略版本号。
     */
    private Integer strategyVersion;
    /**
     * 主体标识的哈希值，用于查询、图谱关联和隐私保护。
     */
    private String subjectHash;
    /**
     * 返回给调用方的最终决策动作。
     */
    private String decision;
    /**
     * 经过信号合并和运行模式投影后的最终风险分。
     */
    private Integer score;
    /**
     * 根据风险分归档得到的风险等级。
     */
    private String riskBand;
    /**
     * 策略版本执行时的运行模式。
     */
    private String mode;
    /**
     * 本次决策端到端耗时，单位毫秒。
     */
    private Integer latencyMs;
    /**
     * 执行记录状态，例如 SUCCEEDED。
     */
    private String status;
    /**
     * 脱敏后的输入快照，供审计使用但不保存原始敏感标识。
     */
    private String inputSnapshotJson;
    /**
     * 序列化后的公开响应，用于幂等重放时直接返回一致结果。
     */
    private String outputJson;
    /**
     * 决策执行记录创建时间。
     */
    private LocalDateTime createdAt;
}
