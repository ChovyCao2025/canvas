package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AiUserDecisionRecommendationDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("ai_user_decision_recommendation")
public class AiUserDecisionRecommendationDO {

    /** AI用户决策推荐主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属租户 ID */
    private Long tenantId;
    /** 关联的运行 ID */
    private Long runId;
    /** 关联的用户 ID */
    private String userId;
    /** AI用户决策推荐模型标识 */
    private String modelKey;
    /** AI用户决策推荐模型版本 */
    private String modelVersion;
    /** AI用户决策推荐决策范围 */
    private String decisionScope;
    /** AI用户决策推荐决策类型 */
    private String decisionType;
    /** AI用户决策推荐决策业务键 */
    private String decisionKey;
    /** AI用户决策推荐动作业务键 */
    private String actionKey;
    /** AI用户决策推荐权益业务键 */
    private String offerKey;
    /** AI用户决策推荐触达渠道 */
    private String channel;
    /** AI用户决策推荐评分 */
    private BigDecimal score;
    /** AI用户决策推荐置信度 */
    private BigDecimal confidence;
    /** AI用户决策推荐推荐排名 */
    private Integer recommendationRank;
    /** AI用户决策推荐预算成本 */
    private BigDecimal budgetCost;
    /** AI用户决策推荐资格状态 */
    private String eligibilityStatus;
    /** AI用户决策推荐兜底原因 */
    private String fallbackReason;
    /** AI用户决策推荐模型特征 JSON */
    private String featureJson;
    /** AI用户决策推荐推荐解释 JSON */
    private String explanationJson;
    /** AI用户决策推荐创建时间 */
    private LocalDateTime createdAt;
    /** AI用户决策推荐最后更新时间 */
    private LocalDateTime updatedAt;
}
