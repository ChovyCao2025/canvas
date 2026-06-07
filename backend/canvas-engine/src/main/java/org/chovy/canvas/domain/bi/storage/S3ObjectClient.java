package org.chovy.canvas.domain.bi.storage;

public interface S3ObjectClient {

    void putObject(S3ObjectRequest request, byte[] bytes);

    byte[] getObject(S3ObjectRequest request);

    boolean deleteObject(S3ObjectRequest request);

    default void putBucketLifecycle(S3BucketLifecycleRequest request, String lifecycleXml) {
        throw new UnsupportedOperationException("S3 bucket lifecycle is not supported");
    }
}
