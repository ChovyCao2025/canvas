package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_decision_feedback")
public class AiDecisionFeedbackDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long recommendationId;
    private String feedbackType;
    private BigDecimal outcomeValue;
    private String metadataJson;
    private String createdBy;
    private LocalDateTime occurredAt;
    private LocalDateTime createdAt;
}
