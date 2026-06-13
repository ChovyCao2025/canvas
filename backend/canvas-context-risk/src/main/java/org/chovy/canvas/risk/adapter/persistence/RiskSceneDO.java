package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控场景实体，定义某类实时决策入口的默认运行模式、失败策略和延迟预算。
 */
@Data
@TableName("risk_scene")
public class RiskSceneDO {

    /**
     * 风控场景记录的自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 场景所属租户。
     */
    private Long tenantId;
    /**
     * 场景业务键，决策请求和策略绑定都通过该键定位场景。
     */
    private String sceneKey;
    /**
     * 场景展示名称。
     */
    private String name;
    /**
     * 场景关联的事件结构定义键，用于校验请求事件字段。
     */
    private String eventSchemaKey;
    /**
     * 场景生命周期状态。
     */
    private String status;
    /**
     * 场景默认运行模式，策略未覆盖时使用。
     */
    private String defaultMode;
    /**
     * 场景默认失败策略，运行时依赖失败时使用。
     */
    private String failPolicy;
    /**
     * 场景允许的决策延迟预算，单位毫秒。
     */
    private Integer latencyBudgetMs;
    /**
     * 场景负责人或维护方。
     */
    private String owner;
    /**
     * 场景创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 场景最后更新时间。
     */
    private LocalDateTime updatedAt;
}
