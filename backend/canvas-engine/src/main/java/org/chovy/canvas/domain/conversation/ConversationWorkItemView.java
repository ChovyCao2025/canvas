package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ConversationWorkItemView 承载 domain.conversation 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param sessionId sessionId 字段。
 * @param contactProfileId contactProfileId 字段。
 * @param userId userId 字段。
 * @param channel channel 字段。
 * @param provider provider 字段。
 * @param subject subject 字段。
 * @param status status 字段。
 * @param priority priority 字段。
 * @param assignedTo assignedTo 字段。
 * @param assignedTeam assignedTeam 字段。
 * @param source source 字段。
 * @param slaDueAt slaDueAt 字段。
 * @param nextFollowUpAt nextFollowUpAt 字段。
 * @param lastCustomerMessageAt lastCustomerMessageAt 字段。
 * @param lastOperatorActivityAt lastOperatorActivityAt 字段。
 * @param tags tags 字段。
 * @param attributes attributes 字段。
 * @param routingStatus routingStatus 字段。
 * @param requiredSkills requiredSkills 字段。
 * @param routingReason routingReason 字段。
 * @param routedAt routedAt 字段。
 * @param slaPolicyKey slaPolicyKey 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record ConversationWorkItemView(
        Long id,
        Long tenantId,
        Long sessionId,
        Long contactProfileId,
        String userId,
        String channel,
        String provider,
        String subject,
        String status,
        String priority,
        String assignedTo,
        String assignedTeam,
        String source,
        LocalDateTime slaDueAt,
        LocalDateTime nextFollowUpAt,
        LocalDateTime lastCustomerMessageAt,
        LocalDateTime lastOperatorActivityAt,
        List<String> tags,
        Map<String, Object> attributes,
        String routingStatus,
        List<String> requiredSkills,
        String routingReason,
        LocalDateTime routedAt,
        String slaPolicyKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public ConversationWorkItemView {
        tags = tags == null ? List.of() : List.copyOf(tags);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        requiredSkills = requiredSkills == null ? List.of() : List.copyOf(requiredSkills);
    }

    /**
     * 创建 ConversationWorkItemView 实例并注入 domain.conversation 场景依赖。
     * @param id 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param sessionId 业务对象 ID，用于定位具体记录。
     * @param contactProfileId 业务对象 ID，用于定位具体记录。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 ConversationWorkItemView 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 ConversationWorkItemView 流程中的校验、计算或对象转换。
     * @param subject 待处理业务值，用于规则计算、转换或外部调用。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param priority priority 参数，用于 ConversationWorkItemView 流程中的校验、计算或对象转换。
     * @param assignedTo 时间或范围边界，用于限定统计窗口。
     * @param assignedTeam assigned team 参数，用于 ConversationWorkItemView 流程中的校验、计算或对象转换。
     * @param source source 参数，用于 ConversationWorkItemView 流程中的校验、计算或对象转换。
     * @param slaDueAt 时间参数，用于计算窗口、过期或审计时间。
     * @param nextFollowUpAt 时间参数，用于计算窗口、过期或审计时间。
     * @param lastCustomerMessageAt 时间参数，用于计算窗口、过期或审计时间。
     * @param lastOperatorActivityAt 时间参数，用于计算窗口、过期或审计时间。
     * @param tags tags 参数，用于 ConversationWorkItemView 流程中的校验、计算或对象转换。
     * @param attributes attributes 参数，用于 ConversationWorkItemView 流程中的校验、计算或对象转换。
     * @param createdAt 时间参数，用于计算窗口、过期或审计时间。
     * @param updatedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    public ConversationWorkItemView(Long id,
                                    Long tenantId,
                                    Long sessionId,
                                    Long contactProfileId,
                                    String userId,
                                    String channel,
                                    String provider,
                                    String subject,
                                    String status,
                                    String priority,
                                    String assignedTo,
                                    String assignedTeam,
                                    String source,
                                    LocalDateTime slaDueAt,
                                    LocalDateTime nextFollowUpAt,
                                    LocalDateTime lastCustomerMessageAt,
                                    LocalDateTime lastOperatorActivityAt,
                                    List<String> tags,
                                    Map<String, Object> attributes,
                                    LocalDateTime createdAt,
                                    LocalDateTime updatedAt) {
        this(id, tenantId, sessionId, contactProfileId, userId, channel, provider, subject, status, priority,
                assignedTo, assignedTeam, source, slaDueAt, nextFollowUpAt, lastCustomerMessageAt,
                lastOperatorActivityAt, tags, attributes, "UNROUTED", List.of(), null, null, null,
                createdAt, updatedAt);
    }
}
