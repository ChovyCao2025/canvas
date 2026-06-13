package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
@TableName("conversation_work_item_audit")
public class ConversationWorkItemAuditDO {
    @TableId(type = IdType.AUTO)
    Long id;
    Long tenantId;
    Long workItemId;
    String eventType;
    String actor;
    String oldValueJson;
    String newValueJson;
    String note;
    LocalDateTime createdAt;
}
