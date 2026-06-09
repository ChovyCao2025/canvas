package org.chovy.canvas.dto.cdp;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * TrackEventReq 承载 dto.cdp 场景中的不可变数据快照。
 * @param messageId messageId 字段。
 * @param type type 字段。
 * @param event event 字段。
 * @param userId userId 字段。
 * @param anonymousId anonymousId 字段。
 * @param idempotencyKey idempotencyKey 字段。
 * @param properties properties 字段。
 * @param context context 字段。
 * @param timestamp timestamp 字段。
 * @param sentAt sentAt 字段。
 */
public record TrackEventReq(
        String messageId,
        String type,
        String event,
        String userId,
        String anonymousId,
        String idempotencyKey,
        Map<String, Object> properties,
        Map<String, Object> context,
        OffsetDateTime timestamp,
        OffsetDateTime sentAt
) {
}
