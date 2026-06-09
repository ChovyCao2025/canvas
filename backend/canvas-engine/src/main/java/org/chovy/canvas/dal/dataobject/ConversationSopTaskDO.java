package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationSopTaskDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_sop_task")
public class ConversationSopTaskDO {

    /** 会话SOP任务主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作事项 ID */
    private Long workItemId;

    /** 会话SOP任务任务业务键 */
    private String taskKey;

    /** 会话SOP任务标题 */
    private String title;

    /** 会话SOP任务当前状态 */
    private String status;

    /** 会话SOP任务负责人 */
    private String assignee;

    /** 会话SOP任务截止时间 */
    private LocalDateTime dueAt;

    /** 会话SOP任务完成人 */
    private String completedBy;

    /** 会话SOP任务完成时间 */
    private LocalDateTime completedAt;

    /** 会话SOP任务扩展元数据 JSON */
    private String metadataJson;

    /** 会话SOP任务创建时间 */
    private LocalDateTime createdAt;

    /** 会话SOP任务最后更新时间 */
    private LocalDateTime updatedAt;
}
