package org.chovy.canvas.domain.bi.storage;

public record S3BucketLifecycleRequest(String bucket) {

    public S3BucketLifecycleRequest {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("bucket is required");
        }
        bucket = bucket.trim();
    }
}
