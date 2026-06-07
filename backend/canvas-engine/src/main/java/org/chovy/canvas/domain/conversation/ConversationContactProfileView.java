package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ConversationContactProfileView(
        Long id,
        Long tenantId,
        String userId,
        String displayName,
        String externalContactId,
        String privateDomainSource,
        String owner,
        String lifecycleStage,
        List<String> tags,
        Map<String, Object> attributes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public ConversationContactProfileView {
        tags = tags == null ? List.of() : List.copyOf(tags);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
