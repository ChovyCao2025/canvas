package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_usage_audit")
public class AiUsageAuditDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long canvasId;

    private String executionId;

    private String nodeId;

    private Long providerId;

    private Long templateId;

    private String modelKey;

    private String status;

    private Integer fallbackUsed;

    private Long latencyMs;

    private Integer promptTokens;

    private Integer completionTokens;

    private String renderedPromptHash;

    private String outputJson;

    private String errorCode;

    private String errorMessage;

    private LocalDateTime createdAt;
}
