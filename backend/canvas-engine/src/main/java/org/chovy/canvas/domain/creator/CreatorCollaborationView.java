package org.chovy.canvas.domain.creator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record CreatorCollaborationView(
        Long id,
        Long tenantId,
        Long campaignId,
        Long creatorId,
        String offerType,
        BigDecimal fixedFeeAmount,
        BigDecimal commissionRate,
        String trackingLink,
        String discountCode,
        String status,
        Map<String, Object> permissionsMetadata,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public CreatorCollaborationView {
        permissionsMetadata = permissionsMetadata == null ? Map.of() : Map.copyOf(permissionsMetadata);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
