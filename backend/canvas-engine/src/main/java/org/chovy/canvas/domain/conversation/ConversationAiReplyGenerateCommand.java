package org.chovy.canvas.domain.conversation;

import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * ConversationAiReplyGenerateCommand 承载 domain.conversation 场景中的不可变数据快照。
 * @param providerId providerId 字段。
 * @param templateId templateId 字段。
 * @param modelKey modelKey 字段。
 * @param tone tone 字段。
 * @param intent intent 字段。
 * @param params params 字段。
 * @param timeoutMs timeoutMs 字段。
 * @param operatorInstruction operatorInstruction 字段。
 */
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
