package org.chovy.canvas.bi.api;
/**
 * BiDeliveryAttachmentDownload 下载结果。
 */
public record BiDeliveryAttachmentDownload(
        /**
         * 文件名。
         */
        String filename,
        /**
         * 内容类型。
         */
        String contentType,
        byte[] bytes) {

    public BiDeliveryAttachmentDownload {
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
