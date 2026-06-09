package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationRoutingAgentDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_routing_agent")
public class ConversationRoutingAgentDO {

    /** 会话路由智能体主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 会话路由智能体智能体业务键 */
    private String agentKey;

    /** 会话路由智能体展示名称 */
    private String displayName;

    /** 会话路由智能体团队业务键 */
    private String teamKey;

    /** 会话路由智能体当前状态 */
    private String status;

    /** 会话路由智能体最大容量 */
    private Integer maxCapacity;

    /** 会话路由智能体当前加载 */
    private Integer currentLoad;

    /** 会话路由智能体技能明细 JSON */
    private String skillsJson;

    /** 会话路由智能体扩展元数据 JSON */
    private String metadataJson;

    /** 会话路由智能体创建人 */
    private String createdBy;

    /** 会话路由智能体创建时间 */
    private LocalDateTime createdAt;

    /** 会话路由智能体最后更新时间 */
    private LocalDateTime updatedAt;
}
