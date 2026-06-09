package org.chovy.canvas.domain.conversation;

import java.util.List;
import java.util.Map;

/**
 * PrivateDomainContactSnapshot 承载 domain.conversation 场景中的不可变数据快照。
 * @param externalContactId externalContactId 字段。
 * @param displayName displayName 字段。
 * @param avatarUrl avatarUrl 字段。
 * @param corpName corpName 字段。
 * @param gender gender 字段。
 * @param unionIdHash unionIdHash 字段。
 * @param ownerUserId ownerUserId 字段。
 * @param remark remark 字段。
 * @param state state 字段。
 * @param addWay addWay 字段。
 * @param tags tags 字段。
 * @param attributes attributes 字段。
 * @param rawPayload rawPayload 字段。
 */
public record PrivateDomainContactSnapshot(
        String externalContactId,
        String displayName,
        String avatarUrl,
        String corpName,
        String gender,
        String unionIdHash,
        String ownerUserId,
        String remark,
        String state,
        String addWay,
        List<String> tags,
        Map<String, Object> attributes,
        Map<String, Object> rawPayload) {

    public PrivateDomainContactSnapshot {
        tags = tags == null ? List.of() : List.copyOf(tags);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        rawPayload = rawPayload == null ? Map.of() : Map.copyOf(rawPayload);
    }
}
