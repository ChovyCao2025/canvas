package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ConversationWorkItemDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("conversation_work_item")
public class ConversationWorkItemDO {

    /** 会话工作事项主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的会话 ID */
    private Long sessionId;

    /** 关联的联系人画像 ID */
    private Long contactProfileId;

    /** 关联的用户 ID */
    private String userId;

    /** 会话工作事项触达渠道 */
    private String channel;

    /** 会话工作事项服务商 */
    private String provider;

    /** 会话工作事项主体 */
    private String subject;

    /** 会话工作事项当前状态 */
    private String status;

    /** 会话工作事项优先级 */
    private String priority;

    /** 会话工作事项分配目标 */
    private String assignedTo;

    /** 会话工作事项分配团队 */
    private String assignedTeam;

    /** 会话工作事项来源 */
    private String source;

    /** 会话工作事项SLA截止时间 */
    private LocalDateTime slaDueAt;

    /** 会话工作事项下次跟进跟进时间 */
    private LocalDateTime nextFollowUpAt;

    /** 会话工作事项最近客户消息时间 */
    private LocalDateTime lastCustomerMessageAt;

    /** 会话工作事项最近操作员活动时间 */
    private LocalDateTime lastOperatorActivityAt;

    /** 会话工作事项标签列表 JSON */
    private String tagsJson;

    /** 会话工作事项事件属性 JSON */
    private String attributesJson;

    /** 会话工作事项路由状态 */
    private String routingStatus;

    /** 会话工作事项要求技能明细 JSON */
    private String requiredSkillsJson;

    /** 会话工作事项路由原因 */
    private String routingReason;

    /** 会话工作事项路由时间 */
    private LocalDateTime routedAt;

    /** 会话工作事项SLA策略业务键 */
    private String slaPolicyKey;

    /** 会话工作事项创建时间 */
    private LocalDateTime createdAt;

    /** 会话工作事项最后更新时间 */
    private LocalDateTime updatedAt;
}
