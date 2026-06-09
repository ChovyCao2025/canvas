package org.chovy.canvas.domain.paidmedia;

import java.util.List;
import java.util.Map;

/**
 * PaidMediaAudienceSyncCommand 承载 domain.paidmedia 场景中的不可变数据快照。
 * @param destinationId destinationId 字段。
 * @param audienceId audienceId 字段。
 * @param userIds userIds 字段。
 * @param externalOperationId externalOperationId 字段。
 * @param metadata metadata 字段。
 */
public record PaidMediaAudienceSyncCommand(
        Long destinationId,
        Long audienceId,
        List<String> userIds,
        String externalOperationId,
        Map<String, Object> metadata) {

    public PaidMediaAudienceSyncCommand {
        userIds = userIds == null ? List.of() : List.copyOf(userIds);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
