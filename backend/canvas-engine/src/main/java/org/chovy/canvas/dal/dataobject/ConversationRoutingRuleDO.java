package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationRoutingRuleDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_routing_rule")
public class ConversationRoutingRuleDO {

    /** 会话路由规则主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 会话路由规则规则业务键 */
    private String ruleKey;

    /** 会话路由规则触达渠道 */
    private String channel;

    /** 会话路由规则最小优先级 */
    private String minPriority;

    /** 会话路由规则要求技能明细 JSON */
    private String requiredSkillsJson;

    /** 会话路由规则目标团队 */
    private String targetTeam;

    /** 会话路由规则SLA分钟 */
    private Integer slaMinutes;

    /** 会话路由规则是否启用 */
    private Integer enabled;

    /** 会话路由规则排序序号 */
    private Integer sortOrder;

    /** 会话路由规则扩展元数据 JSON */
    private String metadataJson;

    /** 会话路由规则创建人 */
    private String createdBy;

    /** 会话路由规则创建时间 */
    private LocalDateTime createdAt;

    /** 会话路由规则最后更新时间 */
    private LocalDateTime updatedAt;
}
