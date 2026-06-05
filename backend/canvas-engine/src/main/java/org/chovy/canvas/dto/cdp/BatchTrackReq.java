package org.chovy.canvas.dto.cdp;

import java.time.OffsetDateTime;
import java.util.List;

public record BatchTrackReq(List<TrackEventReq> batch, OffsetDateTime sentAt) {
}
