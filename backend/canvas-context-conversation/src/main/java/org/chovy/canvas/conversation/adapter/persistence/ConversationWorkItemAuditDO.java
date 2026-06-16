package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * `conversation_work_item_audit` 表的工单审计持久化对象。
 */
@TableName("conversation_work_item_audit")
public class ConversationWorkItemAuditDO {
    /**
     * 工单审计记录的数据库主键。
     */
    @TableId(type = IdType.AUTO)
    Long id;

    /**
     * 隔离审计数据的租户标识。
     */
    Long tenantId;

    /**
     * 审计事件所属工单标识。
     */
    Long workItemId;

    /**
     * 工单事件类型。
     */
    String eventType;

    /**
     * 触发审计事件的操作者。
     */
    String actor;

    /**
     * 变更前值的 JSON 表示。
     */
    String oldValueJson;

    /**
     * 变更后值的 JSON 表示。
     */
    String newValueJson;

    /**
     * 审计事件的人工说明。
     */
    String note;

    /**
     * 审计事件创建时间。
     */
    LocalDateTime createdAt;
}
