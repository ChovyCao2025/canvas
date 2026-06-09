package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationWorkItemAuditDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_work_item_audit")
public class ConversationWorkItemAuditDO {

    /** 会话工作事项审计主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作事项 ID */
    private Long workItemId;

    /** 会话工作事项审计事件类型 */
    private String eventType;

    /** 会话工作事项审计操作人 */
    private String actor;

    /** 会话工作事项审计原值明细 JSON */
    private String oldValueJson;

    /** 会话工作事项审计新值明细 JSON */
    private String newValueJson;

    /** 会话工作事项审计备注 */
    private String note;

    /** 会话工作事项审计创建时间 */
    private LocalDateTime createdAt;
}
