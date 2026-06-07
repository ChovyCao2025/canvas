package org.chovy.canvas.domain.bi.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class BiFileStorageConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BiFileStorageConfiguration.class);

    @Test
    void defaultConfigurationDoesNotCreateGlobalStorageBean() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(BiFileStorage.class));
    }

    @Test
    void s3ProviderConfigurationCreatesS3CompatibleStorageBean() {
        contextRunner
                .withPropertyValues(
                        "canvas.bi.storage.provider=s3",
                        "canvas.bi.storage.s3.endpoint=https://minio.example.test",
                        "canvas.bi.storage.s3.region=us-east-1",
                        "canvas.bi.storage.s3.bucket=canvas-bi",
                        "canvas.bi.storage.s3.access-key=access",
                        "canvas.bi.storage.s3.secret-key=secret",
                        "canvas.bi.storage.s3.key-prefix=bi-artifacts",
                        "canvas.bi.storage.s3.path-style=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(BiFileStorage.class);
                    assertThat(context.getBean(BiFileStorage.class))
                            .isInstanceOf(S3CompatibleBiFileStorage.class);
                    assertThat(context.getBean(BiFileStorage.class).provider()).isEqualTo("S3");
                });
    }

    @Test
    void s3ProviderConfigurationCanApplyLifecyclePolicy() {
        AtomicReference<RecordingS3ObjectClient> clientRef = new AtomicReference<>();
        contextRunner
                .withBean(S3ObjectClient.class, () -> {
                    RecordingS3ObjectClient client = new RecordingS3ObjectClient();
                    clientRef.set(client);
                    return client;
                })
                .withPropertyValues(
                        "canvas.bi.storage.provider=s3",
                        "canvas.bi.storage.s3.endpoint=https://minio.example.test",
                        "canvas.bi.storage.s3.region=us-east-1",
                        "canvas.bi.storage.s3.bucket=canvas-bi",
                        "canvas.bi.storage.s3.access-key=access",
                        "canvas.bi.storage.s3.secret-key=secret",
                        "canvas.bi.storage.s3.key-prefix=bi-artifacts",
                        "canvas.bi.storage.s3.path-style=true",
                        "canvas.bi.storage.s3.lifecycle.enabled=true",
                        "canvas.bi.export.retention-days=9",
                        "canvas.bi.delivery.attachment.retention-days=4")
                .run(context -> {
                    assertThat(context).hasSingleBean(BiFileStorage.class);
                    assertThat(clientRef.get().lifecycleXml)
                            .contains("<Prefix>bi-artifacts/exports/</Prefix>")
                            .contains("<Days>9</Days>")
                            .contains("<Prefix>bi-artifacts/attachments/</Prefix>")
                            .contains("<Days>4</Days>");
                });
    }

    private static final class RecordingS3ObjectClient implements S3ObjectClient {

        private String lifecycleXml;

        @Override
        public void putObject(S3ObjectRequest request, byte[] bytes) {
        }

        @Override
        public byte[] getObject(S3ObjectRequest request) {
            return null;
        }

        @Override
        public boolean deleteObject(S3ObjectRequest request) {
            return false;
        }

        @Override
        public void putBucketLifecycle(S3BucketLifecycleRequest request, String lifecycleXml) {
            this.lifecycleXml = lifecycleXml;
        }
    }
}
