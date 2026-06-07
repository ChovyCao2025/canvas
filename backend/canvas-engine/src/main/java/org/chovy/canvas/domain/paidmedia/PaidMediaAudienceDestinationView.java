package org.chovy.canvas.domain.paidmedia;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PaidMediaAudienceDestinationView(
        Long id,
        Long tenantId,
        String provider,
        String destinationKey,
        String displayName,
        String accountId,
        String externalAudienceId,
        List<String> identifierTypes,
        String consentChannel,
        boolean enforceConsent,
        boolean enabled,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public PaidMediaAudienceDestinationView {
        identifierTypes = identifierTypes == null ? List.of() : List.copyOf(identifierTypes);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public PaidMediaAudienceDestinationView(Long id,
                                            Long tenantId,
                                            String provider,
                                            String destinationKey,
                                            String displayName,
                                            String accountId,
                                            String externalAudienceId,
                                            List<String> identifierTypes,
                                            String consentChannel,
                                            boolean enforceConsent,
                                            boolean enabled,
                                            Map<?, ?> metadata,
                                            LocalDateTime updatedAt) {
        this(id, tenantId, provider, destinationKey, displayName, accountId, externalAudienceId, identifierTypes,
                consentChannel, enforceConsent, enabled, copyMetadata(metadata), null, updatedAt, updatedAt);
    }

    private static Map<String, Object> copyMetadata(Map<?, ?> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return metadata.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue,
                        (left, right) -> right));
    }
}
