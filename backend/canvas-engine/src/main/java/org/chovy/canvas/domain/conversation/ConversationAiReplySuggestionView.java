package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ConversationAiReplySuggestionView 承载 domain.conversation 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workItemId workItemId 字段。
 * @param sessionId sessionId 字段。
 * @param sourceMessageId sourceMessageId 字段。
 * @param promptContext promptContext 字段。
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
 * @param status status 字段。
 * @param generatedBy generatedBy 字段。
 * @param reviewedBy reviewedBy 字段。
 * @param reviewedAt reviewedAt 字段。
 * @param reviewNote reviewNote 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
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
