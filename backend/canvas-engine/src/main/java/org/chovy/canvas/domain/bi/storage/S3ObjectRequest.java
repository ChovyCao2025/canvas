package org.chovy.canvas.domain.bi.storage;

/**
 * S3ObjectRequest 承载 domain.bi.storage 场景中的不可变数据快照。
 * @param bucket bucket 字段。
 * @param objectKey objectKey 字段。
 */
public record S3ObjectRequest(String bucket, String objectKey) {
}
