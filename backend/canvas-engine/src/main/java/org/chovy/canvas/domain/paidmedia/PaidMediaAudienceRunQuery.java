package org.chovy.canvas.domain.paidmedia;

/**
 * PaidMediaAudienceRunQuery 承载 domain.paidmedia 场景中的不可变数据快照。
 * @param destinationId destinationId 字段。
 * @param audienceId audienceId 字段。
 * @param status status 字段。
 * @param limit limit 字段。
 */
public record PaidMediaAudienceRunQuery(
        Long destinationId,
        Long audienceId,
        String status,
        int limit) {
}
