package org.chovy.canvas.domain.conversation;

/**
 * ConversationAdapterContext 承载 domain.conversation 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param operator operator 字段。
 */
public record ConversationAdapterContext(Long tenantId, String operator) {
}
