package org.chovy.canvas.domain.bi.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class S3CompatibleBiFileStorageTest {

    @Test
    void writesReadsAndDeletesObjectsThroughConfiguredBucketAndPrefix() {
        RecordingS3ObjectClient client = new RecordingS3ObjectClient();
        S3CompatibleBiStorageProperties properties = new S3CompatibleBiStorageProperties(
                "https://minio.example.test",
                "us-east-1",
                "canvas-bi",
                "access",
                "secret",
                "bi-artifacts/",
                true,
                "",
                1000,
                3000);
        S3CompatibleBiFileStorage storage = new S3CompatibleBiFileStorage(properties, client);

        BiStoredFile storedFile = storage.write("exports/tenant-7/export-70.csv", "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8));

        assertThat(storedFile.provider()).isEqualTo("S3");
        assertThat(storedFile.key()).isEqualTo("exports/tenant-7/export-70.csv");
        assertThat(storedFile.path()).isEqualTo("s3://canvas-bi/bi-artifacts/exports/tenant-7/export-70.csv");
        assertThat(storedFile.sizeBytes()).isEqualTo(8L);
        assertThat(client.objects).containsEntry("canvas-bi/bi-artifacts/exports/tenant-7/export-70.csv",
                "a,b\n1,2\n".getBytes(StandardCharsets.UTF_8));

        assertThat(storage.read("exports/tenant-7/export-70.csv"))
                .isEqualTo("a,b\n1,2\n".getBytes(StandardCharsets.UTF_8));
        assertThat(storage.delete("exports/tenant-7/export-70.csv")).isTrue();
        assertThat(client.objects).isEmpty();
    }

    @Test
    void rejectsBlankAbsoluteAndTraversalStorageKeys() {
        S3CompatibleBiFileStorage storage = new S3CompatibleBiFileStorage(
                new S3CompatibleBiStorageProperties(
                        "https://s3.example.test",
                        "us-east-1",
                        "canvas-bi",
                        "access",
                        "secret",
                        "",
                        true,
                        "",
                        1000,
                        3000),
                new RecordingS3ObjectClient());

        assertThatThrownBy(() -> storage.write("", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storageKey is required");
        assertThatThrownBy(() -> storage.write("/exports/a.csv", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storageKey must be relative");
        assertThatThrownBy(() -> storage.write("exports/../secret.csv", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storageKey must not contain traversal");
    }

    @Test
    void appliesBucketLifecyclePolicyForExportAndAttachmentPrefixes() {
        RecordingS3ObjectClient client = new RecordingS3ObjectClient();
        S3CompatibleBiFileStorage storage = new S3CompatibleBiFileStorage(
                new S3CompatibleBiStorageProperties(
                        "https://s3.example.test",
                        "us-east-1",
                        "canvas-bi",
                        "access",
                        "secret",
                        "bi-artifacts",
                        true,
                        "",
                        1000,
                        3000),
                client);

        storage.applyLifecyclePolicy(7, 3);

        assertThat(client.lifecycleRequest.bucket()).isEqualTo("canvas-bi");
        assertThat(client.lifecycleXml)
                .contains("<ID>canvas-bi-exports-retention</ID>")
                .contains("<Prefix>bi-artifacts/exports/</Prefix>")
                .contains("<Days>7</Days>")
                .contains("<ID>canvas-bi-attachments-retention</ID>")
                .contains("<Prefix>bi-artifacts/attachments/</Prefix>")
                .contains("<Days>3</Days>");
    }

    private static final class RecordingS3ObjectClient implements S3ObjectClient {

        private final Map<String, byte[]> objects = new LinkedHashMap<>();
        private S3BucketLifecycleRequest lifecycleRequest;
        private String lifecycleXml;

        @Override
        public void putObject(S3ObjectRequest request, byte[] bytes) {
            objects.put(request.bucket() + "/" + request.objectKey(), bytes);
        }

        @Override
        public byte[] getObject(S3ObjectRequest request) {
            return objects.get(request.bucket() + "/" + request.objectKey());
        }

        @Override
        public boolean deleteObject(S3ObjectRequest request) {
            return objects.remove(request.bucket() + "/" + request.objectKey()) != null;
        }

        @Override
        public void putBucketLifecycle(S3BucketLifecycleRequest request, String lifecycleXml) {
            this.lifecycleRequest = request;
            this.lifecycleXml = lifecycleXml;
        }
    }
}
