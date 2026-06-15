package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;

public record BiDatasourceHealthSnapshotView(
        String sourceKey,
        String sourceType,
        boolean available,
        String message,
        LocalDateTime checkedAt) {
}
