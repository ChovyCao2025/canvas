package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ChannelFallbackDecisionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("channel_fallback_decision")
public class ChannelFallbackDecisionDO {

    /** 渠道兜底决策主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的执行 ID */
    private String executionId;

    /** 关联的节点 ID */
    private String nodeId;

    /** 渠道兜底决策原始渠道 */
    private String originalChannel;

    /** 渠道兜底决策原始服务商 */
    private String originalProvider;

    /** 渠道兜底决策最终渠道 */
    private String finalChannel;

    /** 渠道兜底决策最终服务商 */
    private String finalProvider;

    /** 渠道兜底决策决策原因 */
    private String decisionReason;

    /** 渠道兜底决策尝试链路明细 JSON */
    private String attemptChainJson;

    /** 渠道兜底决策创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
