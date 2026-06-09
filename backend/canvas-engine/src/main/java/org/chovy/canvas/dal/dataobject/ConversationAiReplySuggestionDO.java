package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationAiReplySuggestionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_ai_reply_suggestion")
public class ConversationAiReplySuggestionDO {

    /** 会话AI回复建议主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作事项 ID */
    private Long workItemId;

    /** 关联的会话 ID */
    private Long sessionId;

    /** 关联的来源消息 ID */
    private Long sourceMessageId;

    /** 会话AI回复建议提示词上下文 JSON */
    private String promptContextJson;

    /** 会话AI回复建议SUGGESTED回复TEXT */
    private String suggestedReplyText;

    /** 会话AI回复建议回复语气 */
    private String tone;

    /** 会话AI回复建议意图 */
    private String intent;

    /** 会话AI回复建议置信度 */
    private Double confidence;

    /** 会话AI回复建议风险标记 JSON */
    private String riskFlagsJson;

    /** 会话AI回复建议溯源片段 JSON */
    private String groundingSnippetsJson;

    /** 关联的服务商 ID */
    private Long providerId;

    /** 关联的模板 ID */
    private Long templateId;

    /** 会话AI回复建议模型标识 */
    private String modelKey;

    /** 会话AI回复建议服务商状态 */
    private String providerStatus;

    /** 会话AI回复建议是否使用兜底回复 */
    private Boolean fallbackUsed;

    /** 会话AI回复建议当前状态 */
    private String status;

    /** 会话AI回复建议生成人 */
    private String generatedBy;

    /** 会话AI回复建议审核人 */
    private String reviewedBy;

    /** 会话AI回复建议审核时间 */
    private LocalDateTime reviewedAt;

    /** 会话AI回复建议REVIEWNOTE */
    private String reviewNote;

    /** 会话AI回复建议创建时间 */
    private LocalDateTime createdAt;

    /** 会话AI回复建议最后更新时间 */
    private LocalDateTime updatedAt;
}
