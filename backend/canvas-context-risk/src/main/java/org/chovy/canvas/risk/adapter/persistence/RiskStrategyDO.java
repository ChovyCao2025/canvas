package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控策略主表实体，保存策略在场景下的当前生命周期和版本指针。
 */
@Data
@TableName("risk_strategy")
public class RiskStrategyDO {

    /**
     * 风控策略记录的自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 策略所属租户。
     */
    private Long tenantId;
    /**
     * 策略绑定的风控场景键。
     */
    private String sceneKey;
    /**
     * 策略业务键，用于版本、运行时缓存和审计关联。
     */
    private String strategyKey;
    /**
     * 策略展示名称。
     */
    private String name;
    /**
     * 策略当前生命周期状态。
     */
    private String status;
    /**
     * 当前对外生效的策略版本号。
     */
    private Integer activeVersion;
    /**
     * 当前草稿版本号。
     */
    private Integer draftVersion;
    /**
     * 策略风险等级，用于决定模拟和审批门槛。
     */
    private String riskLevel;
    /**
     * 策略负责人或创建方。
     */
    private String owner;
    /**
     * 策略创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 策略最后更新时间。
     */
    private LocalDateTime updatedAt;
}
