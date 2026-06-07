package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_work_item_audit")
public class ConversationWorkItemAuditDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long workItemId;

    private String eventType;

    private String actor;

    private String oldValueJson;

    private String newValueJson;

    private String note;

    private LocalDateTime createdAt;
}
