package org.chovy.canvas.domain.bi.subscription;

/**
 * BiDeliveryAttachmentDownload 承载 domain.bi.subscription 场景中的不可变数据快照。
 * @param filename filename 字段。
 * @param contentType contentType 字段。
 * @param bytes bytes 字段。
 */
public record BiDeliveryAttachmentDownload(
        String filename,
        String contentType,
        byte[] bytes
) {
}
