package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_user_decision_recommendation")
public class AiUserDecisionRecommendationDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long runId;
    private String userId;
    private String modelKey;
    private String modelVersion;
    private String decisionScope;
    private String decisionType;
    private String decisionKey;
    private String actionKey;
    private String offerKey;
    private String channel;
    private BigDecimal score;
    private BigDecimal confidence;
    private Integer recommendationRank;
    private BigDecimal budgetCost;
    private String eligibilityStatus;
    private String fallbackReason;
    private String featureJson;
    private String explanationJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
