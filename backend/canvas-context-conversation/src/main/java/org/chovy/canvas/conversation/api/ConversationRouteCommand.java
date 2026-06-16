package org.chovy.canvas.conversation.api;

import java.util.List;

/**
 * 工单路由请求。
 *
 * @param requiredSkills 人工指定的要求技能
 * @param targetTeam 人工指定的目标团队
 * @param slaMinutes 人工指定的 SLA 时长，单位为分钟
 * @param note 路由备注
 */
public record ConversationRouteCommand(
        /**
         * 人工指定的要求技能。
         */
        List<String> requiredSkills,
        /**
         * 人工指定的目标团队。
         */
        String targetTeam,
        /**
         * 人工指定的 SLA 时长，单位为分钟。
         */
        Integer slaMinutes,
        /**
         * 路由备注。
         */
        String note) {
}
