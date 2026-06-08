package org.chovy.canvas.domain.bi.export;

/**
 * BiExportDownload 承载 domain.bi.export 场景中的不可变数据快照。
 * @param filename filename 字段。
 * @param contentType contentType 字段。
 * @param bytes bytes 字段。
 */
public record BiExportDownload(
        String filename,
        String contentType,
        byte[] bytes) {
}
