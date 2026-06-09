package org.chovy.canvas.domain.creator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CreatorProfileView 承载 domain.creator 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param provider provider 字段。
 * @param handle handle 字段。
 * @param handleKey handleKey 字段。
 * @param displayName displayName 字段。
 * @param creatorTier creatorTier 字段。
 * @param primaryChannel primaryChannel 字段。
 * @param followerCount followerCount 字段。
 * @param avgEngagementRate avgEngagementRate 字段。
 * @param tags tags 字段。
 * @param status status 字段。
 * @param riskStatus riskStatus 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record CreatorProfileView(
        Long id,
        Long tenantId,
        String provider,
        String handle,
        String handleKey,
        String displayName,
        String creatorTier,
        String primaryChannel,
        Long followerCount,
        BigDecimal avgEngagementRate,
        List<String> tags,
        String status,
        String riskStatus,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public CreatorProfileView {
        tags = tags == null ? List.of() : List.copyOf(tags);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
