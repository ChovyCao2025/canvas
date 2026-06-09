package org.chovy.canvas.domain.creator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * CreatorDeliverableView 承载 domain.creator 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param collaborationId collaborationId 字段。
 * @param campaignId campaignId 字段。
 * @param creatorId creatorId 字段。
 * @param deliverableKey deliverableKey 字段。
 * @param contentType contentType 字段。
 * @param platform platform 字段。
 * @param dueAt dueAt 字段。
 * @param postedAt postedAt 字段。
 * @param contentUrl contentUrl 字段。
 * @param status status 字段。
 * @param impressionCount impressionCount 字段。
 * @param likeCount likeCount 字段。
 * @param commentCount commentCount 字段。
 * @param shareCount shareCount 字段。
 * @param saveCount saveCount 字段。
 * @param clickCount clickCount 字段。
 * @param conversionCount conversionCount 字段。
 * @param revenueAmount revenueAmount 字段。
 * @param metadata metadata 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record CreatorDeliverableView(
        Long id,
        Long tenantId,
        Long collaborationId,
        Long campaignId,
        Long creatorId,
        String deliverableKey,
        String contentType,
        String platform,
        LocalDateTime dueAt,
        LocalDateTime postedAt,
        String contentUrl,
        String status,
        Long impressionCount,
        Long likeCount,
        Long commentCount,
        Long shareCount,
        Long saveCount,
        Long clickCount,
        Long conversionCount,
        BigDecimal revenueAmount,
        Map<String, Object> metadata,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public CreatorDeliverableView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
