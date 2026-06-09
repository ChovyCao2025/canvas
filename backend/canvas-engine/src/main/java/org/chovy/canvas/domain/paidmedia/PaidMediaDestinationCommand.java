package org.chovy.canvas.domain.paidmedia;

import java.util.List;
import java.util.Map;

/**
 * PaidMediaDestinationCommand 承载 domain.paidmedia 场景中的不可变数据快照。
 * @param provider provider 字段。
 * @param destinationKey destinationKey 字段。
 * @param displayName displayName 字段。
 * @param accountId accountId 字段。
 * @param externalAudienceId externalAudienceId 字段。
 * @param identifierTypes identifierTypes 字段。
 * @param consentChannel consentChannel 字段。
 * @param enforceConsent enforceConsent 字段。
 * @param enabled enabled 字段。
 * @param metadata metadata 字段。
 */
public record PaidMediaDestinationCommand(
        String provider,
        String destinationKey,
        String displayName,
        String accountId,
        String externalAudienceId,
        List<String> identifierTypes,
        String consentChannel,
        Boolean enforceConsent,
        Boolean enabled,
        Map<String, Object> metadata) {

    public PaidMediaDestinationCommand {
        identifierTypes = identifierTypes == null ? List.of() : List.copyOf(identifierTypes);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
