package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AiDecisionFeedbackDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("ai_decision_feedback")
public class AiDecisionFeedbackDO {

    /** AI决策反馈主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的推荐 ID */
    private Long recommendationId;
    /** AI决策反馈反馈类型 */
    private String feedbackType;
    /** AI决策反馈结果值 */
    private BigDecimal outcomeValue;
    /** AI决策反馈扩展元数据 JSON */
    private String metadataJson;
    /** AI决策反馈创建人 */
    private String createdBy;
    /** AI决策反馈发生时间 */
    private LocalDateTime occurredAt;
    /** AI决策反馈创建时间 */
    private LocalDateTime createdAt;
}
