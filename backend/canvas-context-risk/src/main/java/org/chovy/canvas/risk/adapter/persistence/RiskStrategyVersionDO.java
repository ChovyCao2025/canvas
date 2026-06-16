package org.chovy.canvas.risk.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风控策略版本实体，保存某个版本的规则定义、编译结果、审批信息和生效窗口。
 */
@Data
@TableName("risk_strategy_version")
public class RiskStrategyVersionDO {

    /**
     * 策略版本记录的自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 策略版本所属租户。
     */
    private Long tenantId;

    /**
     * 策略业务键，与策略主表和运行时缓存关联。
     */
    private String strategyKey;

    /**
     * 策略版本号。
     */
    private Integer version;

    /**
     * 该版本的生命周期状态。
     */
    private String status;

    /**
     * 该版本的运行模式。
     */
    private String mode;

    /**
     * 该版本在灰度或双跑场景下的流量比例。
     */
    private BigDecimal trafficPercent;

    /**
     * 编译后可执行策略计划的哈希，用于发布一致性和缓存校验。
     */
    private String compiledHash;

    /**
     * 策略规则定义 JSON，作为编译器输入。
     */
    private String definitionJson;

    /**
     * 规则校验结果 JSON，记录校验是否通过及错误信息。
     */
    private String validationJson;

    /**
     * 创建该版本的操作人。
     */
    private String createdBy;

    /**
     * 提交该版本进入审批的操作人。
     */
    private String submittedBy;

    /**
     * 提交该版本进入审批的时间。
     */
    private LocalDateTime submittedAt;

    /**
     * 审批通过该版本的操作人。
     */
    private String approvedBy;

    /**
     * 该版本审批通过时间。
     */
    private LocalDateTime approvedAt;

    /**
     * 该版本允许开始生效的时间。
     */
    private LocalDateTime effectiveFrom;

    /**
     * 该版本停止生效的时间。
     */
    private LocalDateTime effectiveTo;

    /**
     * 策略版本创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 策略版本最后更新时间。
     */
    private LocalDateTime updatedAt;
}
