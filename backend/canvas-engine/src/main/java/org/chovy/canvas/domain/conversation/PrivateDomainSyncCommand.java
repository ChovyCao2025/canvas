package org.chovy.canvas.domain.conversation;

import java.util.List;
import java.util.Map;

/**
 * PrivateDomainSyncCommand 承载 domain.conversation 场景中的不可变数据快照。
 * @param provider provider 字段。
 * @param syncType syncType 字段。
 * @param sourceCursor sourceCursor 字段。
 * @param nextCursor nextCursor 字段。
 * @param contacts contacts 字段。
 * @param groups groups 字段。
 * @param metadata metadata 字段。
 */
public record PrivateDomainSyncCommand(
        String provider,
        String syncType,
        String sourceCursor,
        String nextCursor,
        List<PrivateDomainContactSnapshot> contacts,
        List<PrivateDomainGroupSnapshot> groups,
        Map<String, Object> metadata) {

    public PrivateDomainSyncCommand {
        contacts = contacts == null ? List.of() : List.copyOf(contacts);
        groups = groups == null ? List.of() : List.copyOf(groups);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
