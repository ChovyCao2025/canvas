package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * PrivateDomainContactView 承载 domain.conversation 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param provider provider 字段。
 * @param externalContactId externalContactId 字段。
 * @param userId userId 字段。
 * @param displayName displayName 字段。
 * @param ownerUserId ownerUserId 字段。
 * @param remark remark 字段。
 * @param state state 字段。
 * @param addWay addWay 字段。
 * @param tags tags 字段。
 * @param attributes attributes 字段。
 * @param syncedAt syncedAt 字段。
 */
public record PrivateDomainContactView(
        Long id,
        Long tenantId,
        String provider,
        String externalContactId,
        String userId,
        String displayName,
        String ownerUserId,
        String remark,
        String state,
        String addWay,
        List<String> tags,
        Map<String, Object> attributes,
        LocalDateTime syncedAt) {

    public PrivateDomainContactView {
        tags = tags == null ? List.of() : List.copyOf(tags);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
