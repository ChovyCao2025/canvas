package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_work_item")
public class ConversationWorkItemDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long sessionId;

    private Long contactProfileId;

    private String userId;

    private String channel;

    private String provider;

    private String subject;

    private String status;

    private String priority;

    private String assignedTo;

    private String assignedTeam;

    private String source;

    private LocalDateTime slaDueAt;

    private LocalDateTime nextFollowUpAt;

    private LocalDateTime lastCustomerMessageAt;

    private LocalDateTime lastOperatorActivityAt;

    private String tagsJson;

    private String attributesJson;

    private String routingStatus;

    private String requiredSkillsJson;

    private String routingReason;

    private LocalDateTime routedAt;

    private String slaPolicyKey;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
