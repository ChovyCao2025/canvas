package org.chovy.canvas.domain.bi.storage;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * BiFileStorageConfiguration 编排 domain.bi.storage 场景的领域业务规则。
 */
@Configuration
public class BiFileStorageConfiguration {

    /**
     * 创建 S3 兼容的 BI 文件存储实现。
     *
     * <p>该 Bean 仅在 {@code canvas.bi.storage.provider=s3} 且没有其它 {@link BiFileStorage} 时启用。
     * 它会读取 endpoint、bucket、凭证、key 前缀、路径风格和超时配置，必要时创建默认 HTTP S3 客户端；
     * 当生命周期配置开启时，会同步写入导出文件和订阅附件的过期规则。</p>
     *
     * @return 用于导出、订阅附件和截图对象的 S3 兼容文件存储
     */
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
        // 准备本次处理所需的上下文和中间变量。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return storage;
    }
}
