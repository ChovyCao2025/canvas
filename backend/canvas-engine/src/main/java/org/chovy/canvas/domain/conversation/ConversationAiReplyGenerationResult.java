package org.chovy.canvas.domain.conversation;

import java.util.List;

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
