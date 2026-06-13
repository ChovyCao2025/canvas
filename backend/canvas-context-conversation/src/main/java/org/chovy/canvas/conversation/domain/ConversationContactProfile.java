package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ConversationContactProfile(
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

    public ConversationContactProfile {
        tags = DomainMaps.copyList(tags);
        attributes = DomainMaps.copy(attributes);
    }

    public ConversationContactProfile withId(Long id) {
        return new ConversationContactProfile(id, tenantId, userId, displayName, externalContactId,
                privateDomainSource, owner, lifecycleStage, tags, attributes, createdAt, updatedAt);
    }
}
