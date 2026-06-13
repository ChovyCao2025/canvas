package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
@TableName("conversation_routing_rule")
public class ConversationRoutingRuleDO {
    @TableId(type = IdType.AUTO)
    Long id;
    Long tenantId;
    String ruleKey;
    String channel;
    String minPriority;
    String requiredSkillsJson;
    String targetTeam;
    Integer slaMinutes;
    Integer enabled;
    Integer sortOrder;
    String metadataJson;
    String createdBy;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
