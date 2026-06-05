package org.chovy.canvas.dto.cdp;

import java.util.List;

public record IngestionResult(int accepted, int rejected, List<IngestionError> errors) {
}
