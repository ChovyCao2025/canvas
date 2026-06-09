package org.chovy.canvas.domain.creator;

import java.math.BigDecimal;
import java.util.Map;

/**
 * CreatorCollaborationCommand 承载 domain.creator 场景中的不可变数据快照。
 * @param campaignId campaignId 字段。
 * @param creatorId creatorId 字段。
 * @param offerType offerType 字段。
 * @param fixedFeeAmount fixedFeeAmount 字段。
 * @param commissionRate commissionRate 字段。
 * @param trackingLink trackingLink 字段。
 * @param discountCode discountCode 字段。
 * @param status status 字段。
 * @param permissionsMetadata permissionsMetadata 字段。
 * @param metadata metadata 字段。
 */
public record CreatorCollaborationCommand(
        Long campaignId,
        Long creatorId,
        String offerType,
        BigDecimal fixedFeeAmount,
        BigDecimal commissionRate,
        String trackingLink,
        String discountCode,
        String status,
        Map<String, Object> permissionsMetadata,
        Map<String, Object> metadata) {

    public CreatorCollaborationCommand {
        permissionsMetadata = permissionsMetadata == null ? Map.of() : Map.copyOf(permissionsMetadata);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
