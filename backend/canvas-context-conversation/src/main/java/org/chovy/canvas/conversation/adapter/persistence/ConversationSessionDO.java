package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
@TableName("conversation_session")
public class ConversationSessionDO {
    @TableId(type = IdType.AUTO)
    Long id;
    Long tenantId;
    Long canvasId;
    Long versionId;
    String executionId;
    String userId;
    String channel;
    String provider;
    String status;
    Integer turnCount;
    String contextJson;
    LocalDateTime lastMessageAt;
    LocalDateTime expiresAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    public Long getTenantId() {
        return tenantId;
    }
    public String getContextJson() {
        return contextJson;
    }
}
