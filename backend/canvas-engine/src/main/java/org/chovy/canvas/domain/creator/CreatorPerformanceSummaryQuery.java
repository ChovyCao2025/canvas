package org.chovy.canvas.domain.creator;

import java.time.LocalDateTime;

/**
 * CreatorPerformanceSummaryQuery 承载 domain.creator 场景中的不可变数据快照。
 * @param campaignId campaignId 字段。
 * @param creatorId creatorId 字段。
 * @param collaborationId collaborationId 字段。
 * @param evaluatedAt evaluatedAt 字段。
 */
public record CreatorPerformanceSummaryQuery(
        Long campaignId,
        Long creatorId,
        Long collaborationId,
        LocalDateTime evaluatedAt) {
}
