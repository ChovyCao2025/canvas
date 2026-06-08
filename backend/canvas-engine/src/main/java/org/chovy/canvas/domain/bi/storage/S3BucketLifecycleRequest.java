package org.chovy.canvas.domain.bi.storage;

/**
 * S3BucketLifecycleRequest 承载 domain.bi.storage 场景中的不可变数据快照。
 * @param bucket bucket 字段。
 */
public record S3BucketLifecycleRequest(String bucket) {

    public S3BucketLifecycleRequest {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("bucket is required");
        }
        bucket = bucket.trim();
    }
}
