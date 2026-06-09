package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ConversationContactProfileView 承载 domain.conversation 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param userId userId 字段。
 * @param displayName displayName 字段。
 * @param externalContactId externalContactId 字段。
 * @param privateDomainSource privateDomainSource 字段。
 * @param owner owner 字段。
 * @param lifecycleStage lifecycleStage 字段。
 * @param tags tags 字段。
 * @param attributes attributes 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
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
