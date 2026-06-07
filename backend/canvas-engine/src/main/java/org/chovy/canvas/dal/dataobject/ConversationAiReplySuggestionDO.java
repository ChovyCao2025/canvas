package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_ai_reply_suggestion")
public class ConversationAiReplySuggestionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workItemId;

    private Long sessionId;

    private Long sourceMessageId;

    private String promptContextJson;

    private String suggestedReplyText;

    private String tone;

    private String intent;

    private Double confidence;

    private String riskFlagsJson;

    private String groundingSnippetsJson;

    private Long providerId;

    private Long templateId;

    private String modelKey;

    private String providerStatus;

    private Boolean fallbackUsed;

    private String status;

    private String generatedBy;

    private String reviewedBy;

    private LocalDateTime reviewedAt;

    private String reviewNote;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
