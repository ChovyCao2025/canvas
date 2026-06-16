package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 可承接会话工单的路由坐席。
 *
 * @param id 坐席标识
 * @param tenantId 租户标识
 * @param agentKey 坐席业务键
 * @param displayName 坐席展示名称
 * @param teamKey 所属团队键
 * @param status 坐席状态
 * @param maxCapacity 最大承接容量
 * @param currentLoad 当前承接数量
 * @param skills 技能标签列表
 * @param metadata 扩展元数据
 * @param createdBy 创建操作者
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ConversationRoutingAgent(
        /**
         * 坐席标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 坐席业务键。
         */
        String agentKey,
        /**
         * 坐席展示名称。
         */
        String displayName,
        /**
         * 所属团队键。
         */
        String teamKey,
        /**
         * 坐席状态。
         */
        String status,
        /**
         * 最大承接容量。
         */
        int maxCapacity,
        /**
         * 当前承接数量。
         */
        int currentLoad,
        /**
         * 技能标签列表。
         */
        List<String> skills,
        /**
         * 扩展元数据。
         */
        Map<String, Object> metadata,
        /**
         * 创建操作者。
         */
        String createdBy,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        /**
         * 更新时间。
         */
        LocalDateTime updatedAt) {

    /**
     * 创建路由坐席并复制可变集合属性。
     */
    public ConversationRoutingAgent {
        skills = DomainMaps.copyList(skills);
        metadata = DomainMaps.copy(metadata);
    }

    /**
     * 返回替换持久化标识后的坐席副本。
     *
     * @param id 持久化生成的坐席标识
     * @return 带新标识的坐席
     */
    public ConversationRoutingAgent withId(Long id) {
        return new ConversationRoutingAgent(id, tenantId, agentKey, displayName, teamKey, status, maxCapacity,
                currentLoad, skills, metadata, createdBy, createdAt, updatedAt);
    }

    /**
     * 返回更新当前承接数量后的坐席副本。
     *
     * @param currentLoad 新的当前承接数量
     * @return 更新承接数量后的坐席
     */
    public ConversationRoutingAgent withCurrentLoad(int currentLoad) {
        // 承接量不能低于 0，避免释放工单时出现负载负数影响路由排序。
        return new ConversationRoutingAgent(id, tenantId, agentKey, displayName, teamKey, status, maxCapacity,
                Math.max(0, currentLoad), skills, metadata, createdBy, createdAt, updatedAt);
    }

    /**
     * 判断坐席是否满足团队、容量和技能要求。
     *
     * @param teamKey 目标团队键
     * @param requiredSkills 路由要求技能
     * @return 坐席可承接时返回 true
     */
    boolean canHandle(String teamKey, List<String> requiredSkills) {
        return "AVAILABLE".equals(status)
                && currentLoad < maxCapacity
                && (teamKey == null || teamKey.equals(this.teamKey))
                && skills.containsAll(requiredSkills);
    }
}
