package org.chovy.canvas.dto.cdp;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * BatchTrackReq 承载 dto.cdp 场景中的不可变数据快照。
 * @param batch batch 字段。
 * @param sentAt sentAt 字段。
 */
public record BatchTrackReq(List<TrackEventReq> batch, OffsetDateTime sentAt) {
}
