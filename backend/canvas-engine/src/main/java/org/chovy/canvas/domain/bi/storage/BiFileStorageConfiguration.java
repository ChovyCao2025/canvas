package org.chovy.canvas.domain.bi.storage;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BiFileStorageConfiguration {

    @Bean
    @ConditionalOnMissingBean(BiFileStorage.class)
    @ConditionalOnProperty(name = "canvas.bi.storage.provider", havingValue = "s3")
    public BiFileStorage s3CompatibleBiFileStorage(
            ObjectProvider<S3ObjectClient> objectClientProvider,
            @Value("${canvas.bi.storage.s3.endpoint}") String endpoint,
            @Value("${canvas.bi.storage.s3.region:us-east-1}") String region,
            @Value("${canvas.bi.storage.s3.bucket}") String bucket,
            @Value("${canvas.bi.storage.s3.access-key}") String accessKey,
            @Value("${canvas.bi.storage.s3.secret-key}") String secretKey,
            @Value("${canvas.bi.storage.s3.key-prefix:}") String keyPrefix,
            @Value("${canvas.bi.storage.s3.path-style:true}") boolean pathStyle,
            @Value("${canvas.bi.storage.s3.public-base-url:}") String publicBaseUrl,
            @Value("${canvas.bi.storage.s3.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${canvas.bi.storage.s3.request-timeout-ms:15000}") int requestTimeoutMs,
            @Value("${canvas.bi.storage.s3.lifecycle.enabled:false}") boolean lifecycleEnabled,
            @Value("${canvas.bi.export.retention-days:7}") int exportRetentionDays,
            @Value("${canvas.bi.delivery.attachment.retention-days:7}") int attachmentRetentionDays) {
        S3CompatibleBiStorageProperties properties = new S3CompatibleBiStorageProperties(
                endpoint,
                region,
                bucket,
                accessKey,
                secretKey,
                keyPrefix,
                pathStyle,
                publicBaseUrl,
                connectTimeoutMs,
                requestTimeoutMs);
        S3ObjectClient objectClient = objectClientProvider.getIfAvailable(() -> {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                    .build();
            return new HttpS3ObjectClient(properties, httpClient);
        });
        S3CompatibleBiFileStorage storage = new S3CompatibleBiFileStorage(properties, objectClient);
        if (lifecycleEnabled) {
            storage.applyLifecyclePolicy(exportRetentionDays, attachmentRetentionDays);
        }
        return storage;
    }
}
