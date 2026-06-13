package org.chovy.canvas.cdp.api;

import java.util.List;

public record CdpIngestionResult(int accepted, int rejected, List<CdpIngestionError> errors) {
}
