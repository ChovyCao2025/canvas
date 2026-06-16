package org.chovy.canvas.conversation.api;

import java.util.List;
import java.util.Map;

/**
 * 新建或更新路由坐席配置的请求。
 *
 * @param agentKey 坐席业务键
 * @param displayName 坐席展示名称
 * @param teamKey 所属团队键
 * @param status 坐席状态
 * @param maxCapacity 最大承接容量
 * @param currentLoad 当前承接数量
 * @param skills 技能标签列表
 * @param metadata 扩展元数据
 */
public record ConversationRoutingAgentCommand(
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
        Integer maxCapacity,
        /**
         * 当前承接数量。
         */
        Integer currentLoad,
        /**
         * 技能标签列表。
         */
        List<String> skills,
        /**
         * 扩展元数据。
         */
        Map<String, Object> metadata) {
}
