package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * `conversation_sla_breach` 表的 SLA 违约持久化对象。
 */
@TableName("conversation_sla_breach")
public class ConversationSlaBreachDO {
    /**
     * SLA 违约记录的数据库主键。
     */
    @TableId(type = IdType.AUTO)
    Long id;

    /**
     * 隔离 SLA 违约数据的租户标识。
     */
    Long tenantId;

    /**
     * 发生 SLA 违约的工单标识。
     */
    Long workItemId;

    /**
     * SLA 违约类型。
     */
    String breachType;

    /**
     * 违约严重程度。
     */
    String severity;

    /**
     * 违约处理状态。
     */
    String status;

    /**
     * 违约升级处理目标。
     */
    String escalationTarget;

    /**
     * 记录违约原因或判定说明。
     */
    String reason;

    /**
     * SLA 原始到期时间。
     */
    LocalDateTime dueAt;

    /**
     * 系统记录违约发生的时间。
     */
    LocalDateTime breachedAt;

    /**
     * 解决违约的操作者。
     */
    String resolvedBy;

    /**
     * 违约被解决的时间。
     */
    LocalDateTime resolvedAt;

    /**
     * 违约扩展上下文的 JSON 表示。
     */
    String metadataJson;

    /**
     * 违约记录创建时间。
     */
    LocalDateTime createdAt;

    /**
     * 违约记录最近更新时间。
     */
    LocalDateTime updatedAt;
}
