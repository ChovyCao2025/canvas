package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;

public record WarehouseWatermark(LocalDateTime watermarkTime, LocalDateTime updatedAt) {
}
