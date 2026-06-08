package org.chovy.canvas.domain.bi.subscription;

/**
 * BiSnapshotRenderResult record.
 * @param format 截图或渲染产物格式，例如 png、pdf.
 * @param contentType 渲染产物 MIME 类型，用于附件和下载响应.
 * @param bytes 渲染产物字节内容，构造和访问时都会复制以避免外部修改.
 */
public record BiSnapshotRenderResult(
        String format,
        String contentType,
        byte[] bytes
) {
    public BiSnapshotRenderResult {
        bytes = bytes == null ? new byte[0] : bytes.clone();
    }

    /**
     * 返回渲染产物内容副本，避免调用方修改 record 内部字节数组。
     *
     * @return 渲染产物字节副本
     */
    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
