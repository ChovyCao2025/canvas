package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
@TableName("conversation_contact_profile")
public class ConversationContactProfileDO {
    @TableId(type = IdType.AUTO)
    Long id;
    Long tenantId;
    String userId;
    String displayName;
    String externalContactId;
    String privateDomainSource;
    String owner;
    String lifecycleStage;
    String tagsJson;
    String attributesJson;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
