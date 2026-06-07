package org.chovy.canvas.domain.conversation;

import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

public record ConversationAiReplyGenerateCommand(
        Long providerId,
        Long templateId,
        String modelKey,
        String tone,
        String intent,
        Map<String, Object> params,
        Integer timeoutMs,
        String operatorInstruction) {

    public ConversationAiReplyGenerateCommand {
        params = params == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }
}
