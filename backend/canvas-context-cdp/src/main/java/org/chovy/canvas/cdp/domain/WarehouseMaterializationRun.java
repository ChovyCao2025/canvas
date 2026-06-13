package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;

public record WarehouseMaterializationRun(String status, LocalDateTime finishedAt, LocalDateTime startedAt) {
}
