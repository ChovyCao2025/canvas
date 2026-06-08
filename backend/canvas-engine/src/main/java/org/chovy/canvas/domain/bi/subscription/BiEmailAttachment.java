package org.chovy.canvas.domain.bi.subscription;

/**
 * BiEmailAttachment record.
 * @param fileName 邮件附件文件名，通常来自 BI 截图或导出文件名.
 * @param contentType 附件 MIME 类型，用于 SMTP 客户端设置内容类型.
 * @param bytes 附件字节内容，构造和访问时都会复制以避免外部修改.
 */
public record BiEmailAttachment(
        String fileName,
        String contentType,
        byte[] bytes
) {
    public BiEmailAttachment {
        bytes = bytes == null ? new byte[0] : bytes.clone();
    }

    /**
     * 返回附件内容副本，避免调用方修改 record 内部字节数组。
     *
     * @return 附件字节副本
     */
    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
