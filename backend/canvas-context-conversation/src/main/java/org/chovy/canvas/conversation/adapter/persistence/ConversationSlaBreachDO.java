package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
@TableName("conversation_sla_breach")
public class ConversationSlaBreachDO {
    @TableId(type = IdType.AUTO)
    Long id;
    Long tenantId;
    Long workItemId;
    String breachType;
    String severity;
    String status;
    String escalationTarget;
    String reason;
    LocalDateTime dueAt;
    LocalDateTime breachedAt;
    String resolvedBy;
    LocalDateTime resolvedAt;
    String metadataJson;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
