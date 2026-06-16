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
    /**
     * 保存 id 对应的风控状态或配置。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 租户编号。 */
    /**
     * 保存 tenantId 对应的风控状态或配置。
     */
    private Long tenantId;
    /** 对外仿真编号。 */
    /**
     * 保存 simulationId 对应的风控状态或配置。
     */
    private String simulationId;
    /** 场景业务键。 */
    /**
     * 保存 sceneKey 对应的风控状态或配置。
     */
    private String sceneKey;
    /** 策略业务键。 */
    /**
     * 保存 strategyKey 对应的风控状态或配置。
     */
    private String strategyKey;
    /** 基线版本。 */
    /**
     * 保存 baselineVersion 对应的风控状态或配置。
     */
    private Integer baselineVersion;
    /** 候选版本。 */
    /**
     * 保存 candidateVersion 对应的风控状态或配置。
     */
    private Integer candidateVersion;
    /** 仿真状态。 */
    /**
     * 保存 status 对应的风控状态或配置。
     */
    private String status;
    /** 样本数量。 */
    /**
     * 保存 sampleSize 对应的风控状态或配置。
     */
    private Integer sampleSize;
    /** 动作变化样本数。 */
    /**
     * 保存 changedActionCount 对应的风控状态或配置。
     */
    private Integer changedActionCount;
    /** 动作分布 JSON。 */
    /**
     * 保存 actionDistributionJson 对应的风控状态或配置。
     */
    private String actionDistributionJson;
    /** 动作变化明细 JSON。 */
    /**
     * 保存 actionChangesJson 对应的风控状态或配置。
     */
    private String actionChangesJson;
    /** 创建时间。 */
    /**
     * 保存 createdAt 对应的风控状态或配置。
     */
    private LocalDateTime createdAt;
}
