package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationSlaBreachDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_sla_breach")
public class ConversationSlaBreachDO {

    /** 会话SLA违约主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的工作事项 ID */
    private Long workItemId;

    /** 会话SLA违约违约类型 */
    private String breachType;

    /** 会话SLA违约严重级别 */
    private String severity;

    /** 会话SLA违约当前状态 */
    private String status;

    /** 会话SLA违约升级目标 */
    private String escalationTarget;

    /** 会话SLA违约原因说明 */
    private String reason;

    /** 会话SLA违约截止时间 */
    private LocalDateTime dueAt;

    /** 会话SLA违约时间 */
    private LocalDateTime breachedAt;

    /** 会话SLA违约解决人 */
    private String resolvedBy;

    /** 会话SLA违约解决时间 */
    private LocalDateTime resolvedAt;

    /** 会话SLA违约扩展元数据 JSON */
    private String metadataJson;

    /** 会话SLA违约创建时间 */
    private LocalDateTime createdAt;

    /** 会话SLA违约最后更新时间 */
    private LocalDateTime updatedAt;
}
