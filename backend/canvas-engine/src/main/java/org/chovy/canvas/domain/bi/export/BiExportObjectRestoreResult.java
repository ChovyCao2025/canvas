package org.chovy.canvas.domain.bi.export;

import java.util.List;

/**
 * BiExportObjectRestoreResult 承载 domain.bi.export 场景中的不可变数据快照。
 * @param exportId exportId 字段。
 * @param primaryProvider primaryProvider 字段。
 * @param fallbackProvider fallbackProvider 字段。
 * @param checkedObjects checkedObjects 字段。
 * @param restoredObjects restoredObjects 字段。
 * @param missingObjects missingObjects 字段。
 * @param restoredKeys restoredKeys 字段。
 * @param missingKeys missingKeys 字段。
 */
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
