package org.chovy.canvas.domain.paidmedia;

import java.util.List;
import java.util.Map;

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
