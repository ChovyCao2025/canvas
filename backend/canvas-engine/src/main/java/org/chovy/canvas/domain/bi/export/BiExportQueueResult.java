package org.chovy.canvas.domain.bi.export;

import java.util.List;

public record BiExportQueueResult(
        int checked,
        int processed,
        int completed,
        int failed,
        List<BiExportJobView> jobs) {
}
