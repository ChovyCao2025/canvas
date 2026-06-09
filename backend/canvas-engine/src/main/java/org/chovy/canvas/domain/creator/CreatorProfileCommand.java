package org.chovy.canvas.domain.creator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * CreatorProfileCommand 承载 domain.creator 场景中的不可变数据快照。
 * @param provider provider 字段。
 * @param handle handle 字段。
 * @param displayName displayName 字段。
 * @param creatorTier creatorTier 字段。
 * @param primaryChannel primaryChannel 字段。
 * @param followerCount followerCount 字段。
 * @param avgEngagementRate avgEngagementRate 字段。
 * @param tags tags 字段。
 * @param status status 字段。
 * @param riskStatus riskStatus 字段。
 * @param metadata metadata 字段。
 */
public record CreatorProfileCommand(
        String provider,
        String handle,
        String displayName,
        String creatorTier,
        String primaryChannel,
        Long followerCount,
        BigDecimal avgEngagementRate,
        List<String> tags,
        String status,
        String riskStatus,
        Map<String, Object> metadata) {

    public CreatorProfileCommand {
        tags = tags == null ? List.of() : List.copyOf(tags);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
