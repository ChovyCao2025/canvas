package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * `conversation_routing_agent` 表的路由坐席持久化对象。
 */
@TableName("conversation_routing_agent")
public class ConversationRoutingAgentDO {
    /**
     * 路由坐席记录的数据库主键。
     */
    @TableId(type = IdType.AUTO)
    Long id;

    /**
     * 隔离坐席配置的租户标识。
     */
    Long tenantId;

    /**
     * 租户内唯一的坐席业务键。
     */
    String agentKey;

    /**
     * 坐席展示名称。
     */
    String displayName;

    /**
     * 坐席所属团队键。
     */
    String teamKey;

    /**
     * 坐席参与路由的状态。
     */
    String status;

    /**
     * 坐席可承接的最大并发工单数。
     */
    Integer maxCapacity;

    /**
     * 坐席当前已承接的工单数量。
     */
    Integer currentLoad;

    /**
     * 坐席技能标签列表的 JSON 表示。
     */
    String skillsJson;

    /**
     * 坐席扩展元数据的 JSON 表示。
     */
    String metadataJson;

    /**
     * 创建该坐席配置的操作者。
     */
    String createdBy;

    /**
     * 坐席配置创建时间。
     */
    LocalDateTime createdAt;

    /**
     * 坐席配置最近更新时间。
     */
    LocalDateTime updatedAt;
}
