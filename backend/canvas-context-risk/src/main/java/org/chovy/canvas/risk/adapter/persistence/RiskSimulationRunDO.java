package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控仿真运行实体，保存实验室仿真结果供工作台历史读取。
 */
@Data
@TableName("risk_simulation_run")
public class RiskSimulationRunDO {

    /** 仿真运行自增主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户编号。 */
    private Long tenantId;
    /** 对外仿真编号。 */
    private String simulationId;
    /** 场景业务键。 */
    private String sceneKey;
    /** 策略业务键。 */
    private String strategyKey;
    /** 基线版本。 */
    private Integer baselineVersion;
    /** 候选版本。 */
    private Integer candidateVersion;
    /** 仿真状态。 */
    private String status;
    /** 样本数量。 */
    private Integer sampleSize;
    /** 动作变化样本数。 */
    private Integer changedActionCount;
    /** 动作分布 JSON。 */
    private String actionDistributionJson;
    /** 动作变化明细 JSON。 */
    private String actionChangesJson;
    /** 创建时间。 */
    private LocalDateTime createdAt;
}
