package org.chovy.canvas.domain.conversation;

/**
 * ConversationIngressResp 承载 domain.conversation 场景中的不可变数据快照。
 * @param sessionId sessionId 字段。
 * @param messageId messageId 字段。
 * @param status status 字段。
 * @param duplicate duplicate 字段。
 * @param resumedWaitCount resumedWaitCount 字段。
 */
public record ConversationIngressResp(
        Long sessionId,
        Long messageId,
        String status,
        boolean duplicate,
        int resumedWaitCount) {
}
