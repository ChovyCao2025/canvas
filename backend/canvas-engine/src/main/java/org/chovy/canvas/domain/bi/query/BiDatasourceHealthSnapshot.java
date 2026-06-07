package org.chovy.canvas.domain.bi.query;

import java.time.LocalDateTime;

public record BiDatasourceHealthSnapshot(
        String sourceKey,
        String sourceType,
        boolean available,
        String message,
        LocalDateTime checkedAt
) {
}
