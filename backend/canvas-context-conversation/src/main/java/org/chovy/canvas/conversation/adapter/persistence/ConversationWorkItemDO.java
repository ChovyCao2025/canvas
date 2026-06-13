package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
@TableName("conversation_work_item")
public class ConversationWorkItemDO {
    @TableId(type = IdType.AUTO)
    Long id;
    Long tenantId;
    Long sessionId;
    Long contactProfileId;
    String userId;
    String channel;
    String provider;
    String subject;
    String status;
    String priority;
    String assignedTo;
    String assignedTeam;
    String source;
    LocalDateTime slaDueAt;
    LocalDateTime nextFollowUpAt;
    LocalDateTime lastCustomerMessageAt;
    LocalDateTime lastOperatorActivityAt;
    String tagsJson;
    String attributesJson;
    String routingStatus;
    String requiredSkillsJson;
    String routingReason;
    LocalDateTime routedAt;
    String slaPolicyKey;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    public String getAttributesJson() {
        return attributesJson;
    }
    public String getRequiredSkillsJson() {
        return requiredSkillsJson;
    }
}
