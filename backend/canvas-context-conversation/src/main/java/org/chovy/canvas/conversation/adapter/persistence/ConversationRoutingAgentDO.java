package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
@TableName("conversation_routing_agent")
public class ConversationRoutingAgentDO {
    @TableId(type = IdType.AUTO)
    Long id;
    Long tenantId;
    String agentKey;
    String displayName;
    String teamKey;
    String status;
    Integer maxCapacity;
    Integer currentLoad;
    String skillsJson;
    String metadataJson;
    String createdBy;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
