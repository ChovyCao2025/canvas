package org.chovy.canvas.cdp.api;

import java.time.OffsetDateTime;
import java.util.List;

public record CdpBatchTrackCommand(List<CdpTrackEventCommand> batch, OffsetDateTime sentAt) {
}
