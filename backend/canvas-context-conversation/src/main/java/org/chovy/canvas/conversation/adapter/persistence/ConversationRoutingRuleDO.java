package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * `conversation_routing_rule` 表的路由规则持久化对象。
 */
@TableName("conversation_routing_rule")
public class ConversationRoutingRuleDO {
    /**
     * 路由规则记录的数据库主键。
     */
    @TableId(type = IdType.AUTO)
    Long id;

    /**
     * 隔离路由规则的租户标识。
     */
    Long tenantId;

    /**
     * 租户内唯一的规则业务键。
     */
    String ruleKey;

    /**
     * 规则适用的会话渠道。
     */
    String channel;

    /**
     * 触发该规则所需的最低工单优先级。
     */
    String minPriority;

    /**
     * 命中规则后要求的技能列表 JSON。
     */
    String requiredSkillsJson;

    /**
     * 命中规则后分配的目标团队。
     */
    String targetTeam;

    /**
     * 命中规则后使用的 SLA 时长，单位为分钟。
     */
    Integer slaMinutes;

    /**
     * 是否启用该规则，数据库以整数保存布尔值。
     */
    Integer enabled;

    /**
     * 多条规则同时匹配时的排序值。
     */
    Integer sortOrder;

    /**
     * 规则扩展配置的 JSON 表示。
     */
    String metadataJson;

    /**
     * 创建该规则的操作者。
     */
    String createdBy;

    /**
     * 规则创建时间。
     */
    LocalDateTime createdAt;

    /**
     * 规则最近更新时间。
     */
    LocalDateTime updatedAt;
}
