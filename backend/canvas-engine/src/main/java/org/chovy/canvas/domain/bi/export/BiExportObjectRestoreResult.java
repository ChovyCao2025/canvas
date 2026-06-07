package org.chovy.canvas.domain.bi.export;

import java.util.List;

public record BiExportObjectRestoreResult(
        Long exportId,
        String primaryProvider,
        String fallbackProvider,
        int checkedObjects,
        int restoredObjects,
        int missingObjects,
        List<String> restoredKeys,
        List<String> missingKeys) {
}
