package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ConversationAiReplySuggestionView(
        Long id,
        Long tenantId,
        Long workItemId,
        Long sessionId,
        Long sourceMessageId,
        Map<String, Object> promptContext,
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
        boolean fallbackUsed,
        String status,
        String generatedBy,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public ConversationAiReplySuggestionView {
        promptContext = promptContext == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(promptContext));
        riskFlags = riskFlags == null ? List.of() : List.copyOf(riskFlags);
        groundingSnippets = groundingSnippets == null ? List.of() : List.copyOf(groundingSnippets);
    }
}
