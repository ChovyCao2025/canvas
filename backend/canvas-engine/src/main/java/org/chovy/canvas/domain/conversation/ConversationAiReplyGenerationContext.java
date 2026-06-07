package org.chovy.canvas.domain.conversation;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

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
