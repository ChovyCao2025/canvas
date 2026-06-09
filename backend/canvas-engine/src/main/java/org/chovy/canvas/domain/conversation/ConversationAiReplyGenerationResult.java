package org.chovy.canvas.domain.conversation;

import java.util.List;

/**
 * ConversationAiReplyGenerationResult 承载 domain.conversation 场景中的不可变数据快照。
 * @param suggestedReplyText suggestedReplyText 字段。
 * @param tone tone 字段。
 * @param intent intent 字段。
 * @param confidence confidence 字段。
 * @param riskFlags riskFlags 字段。
 * @param groundingSnippets groundingSnippets 字段。
 * @param providerId providerId 字段。
 * @param templateId templateId 字段。
 * @param modelKey modelKey 字段。
 * @param providerStatus providerStatus 字段。
 * @param fallbackUsed fallbackUsed 字段。
 */
public record ConversationAiReplyGenerationResult(
        String suggestedReplyText,
        String tone,
        String intent,
        double confidence,
        List<String> riskFlags,
        List<String> groundingSnippets,
        Long providerId,
        Long templateId,
        String modelKey,
        String providerStatus,
        boolean fallbackUsed) {

    public ConversationAiReplyGenerationResult {
        riskFlags = riskFlags == null ? List.of() : List.copyOf(riskFlags);
        groundingSnippets = groundingSnippets == null ? List.of() : List.copyOf(groundingSnippets);
    }
}
