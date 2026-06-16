package org.chovy.canvas.bi.api;
/**
 * BiSelfServiceExportDownload 下载结果。
 */
public record BiSelfServiceExportDownload(
        /**
         * 文件名。
         */
        String filename,
        /**
         * 内容类型。
         */
        String contentType,
        byte[] bytes) {

    public BiSelfServiceExportDownload {
        bytes = bytes == null ? new byte[0] : bytes.clone();
    }
    /**
     * 执行 bytes 相关处理。
     */
    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
