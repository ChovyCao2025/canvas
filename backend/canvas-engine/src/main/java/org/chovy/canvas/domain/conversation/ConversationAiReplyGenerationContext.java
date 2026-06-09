package org.chovy.canvas.domain.conversation;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * ConversationAiReplyGenerationContext 承载 domain.conversation 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param workItem workItem 字段。
 * @param contactProfile contactProfile 字段。
 * @param session session 字段。
 * @param messages messages 字段。
 * @param tasks tasks 字段。
 * @param promptContext promptContext 字段。
 * @param command command 字段。
 */
public record ConversationAiReplyGenerationContext(
        Long tenantId,
        ConversationWorkItemView workItem,
        ConversationContactProfileView contactProfile,
        ConversationSessionView session,
        List<ConversationMessageView> messages,
        List<ConversationSopTaskView> tasks,
        Map<String, Object> promptContext,
        ConversationAiReplyGenerateCommand command) {

    public ConversationAiReplyGenerationContext {
        messages = messages == null ? List.of() : List.copyOf(messages);
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
        promptContext = promptContext == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(promptContext));
    }
}
