package org.chovy.canvas.conversation.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * `conversation_work_item` 表的会话工单持久化对象。
 */
@TableName("conversation_work_item")
public class ConversationWorkItemDO {
    /**
     * 工单记录的数据库主键。
     */
    @TableId(type = IdType.AUTO)
    Long id;

    /**
     * 隔离工单数据的租户标识。
     */
    Long tenantId;

    /**
     * 工单关联的会话标识。
     */
    Long sessionId;

    /**
     * 工单关联的联系人画像标识。
     */
    Long contactProfileId;

    /**
     * 工单对应的用户标识。
     */
    String userId;

    /**
     * 工单来源渠道。
     */
    String channel;

    /**
     * 工单来源服务商。
     */
    String provider;

    /**
     * 工单主题。
     */
    String subject;

    /**
     * 工单处理状态。
     */
    String status;

    /**
     * 工单优先级。
     */
    String priority;

    /**
     * 当前分配的处理人。
     */
    String assignedTo;

    /**
     * 当前分配的处理团队。
     */
    String assignedTeam;

    /**
     * 工单创建来源。
     */
    String source;

    /**
     * 工单 SLA 到期时间。
     */
    LocalDateTime slaDueAt;

    /**
     * 下一次跟进时间。
     */
    LocalDateTime nextFollowUpAt;

    /**
     * 最近一条客户消息时间。
     */
    LocalDateTime lastCustomerMessageAt;

    /**
     * 最近一次运营侧处理时间。
     */
    LocalDateTime lastOperatorActivityAt;

    /**
     * 工单标签列表的 JSON 表示。
     */
    String tagsJson;

    /**
     * 工单扩展属性的 JSON 表示。
     */
    String attributesJson;

    /**
     * 工单路由处理状态。
     */
    String routingStatus;

    /**
     * 工单要求技能列表的 JSON 表示。
     */
    String requiredSkillsJson;

    /**
     * 路由决策原因。
     */
    String routingReason;

    /**
     * 工单完成路由的时间。
     */
    LocalDateTime routedAt;

    /**
     * 工单命中的 SLA 策略键。
     */
    String slaPolicyKey;

    /**
     * 工单创建时间。
     */
    LocalDateTime createdAt;

    /**
     * 工单最近更新时间。
     */
    LocalDateTime updatedAt;

    /**
     * 返回工单属性 JSON，供映射测试验证结构化属性。
     *
     * @return 工单属性 JSON
     */
    public String getAttributesJson() {
        return attributesJson;
    }

    /**
     * 返回工单要求技能 JSON，供映射测试验证路由技能。
     *
     * @return 要求技能 JSON
     */
    public String getRequiredSkillsJson() {
        return requiredSkillsJson;
    }
}
