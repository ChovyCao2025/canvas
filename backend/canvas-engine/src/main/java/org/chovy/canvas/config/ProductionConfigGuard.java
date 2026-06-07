package org.chovy.canvas.config;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Profile("prod")
public class ProductionConfigGuard implements SmartInitializingSingleton {

    private static final String DEFAULT_EVENT_REPORT_SECRET = "canvas-event-report-secret-2026!!";
    private static final String DEFAULT_SECRET_CIPHER_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final int MAX_ASSET_UPLOAD_WEBHOOK_TOLERANCE_SECONDS = 300;

    private final List<String> allowedOrigins;
    private final boolean allowCredentials;
    private final String eventReportSecret;
    private final String jwtSecret;
    private final String datasourceUsername;
    private final String datasourcePassword;
    private final String secretCipherKey;
    private final String assetUploadWebhookSecret;
    private final boolean assetUploadS3Enabled;
    private final String assetUploadS3Endpoint;
    private final String assetUploadS3Bucket;
    private final String assetUploadS3AccessKey;
    private final String assetUploadS3SecretKey;
    private final String assetUploadS3PublicBaseUrl;
    private final boolean assetUploadCleanupEnabled;
    private final Long assetUploadCleanupTenantId;
    private final Integer assetUploadWebhookToleranceSeconds;

    public ProductionConfigGuard(
            @Value("${canvas.cors.allowed-origins:}") List<String> allowedOrigins,
            @Value("${canvas.cors.allow-credentials:true}") boolean allowCredentials,
            @Value("${canvas.events.report-secret:}") String eventReportSecret,
            @Value("${canvas.jwt.secret:}") String jwtSecret,
            @Value("${spring.datasource.username:}") String datasourceUsername,
            @Value("${spring.datasource.password:}") String datasourcePassword,
            @Value("${canvas.secret-cipher.key:}") String secretCipherKey,
            @Value("${canvas.marketing.content.asset-upload.webhook-secret:}") String assetUploadWebhookSecret,
            @Value("${canvas.marketing.content.asset-upload.s3.enabled:false}") boolean assetUploadS3Enabled,
            @Value("${canvas.marketing.content.asset-upload.s3.endpoint:}") String assetUploadS3Endpoint,
            @Value("${canvas.marketing.content.asset-upload.s3.bucket:}") String assetUploadS3Bucket,
            @Value("${canvas.marketing.content.asset-upload.s3.access-key:}") String assetUploadS3AccessKey,
            @Value("${canvas.marketing.content.asset-upload.s3.secret-key:}") String assetUploadS3SecretKey,
            @Value("${canvas.marketing.content.asset-upload.s3.public-base-url:}") String assetUploadS3PublicBaseUrl,
            @Value("${canvas.marketing.content.asset-upload.cleanup.enabled:false}") boolean assetUploadCleanupEnabled,
            @Value("${canvas.marketing.content.asset-upload.cleanup.tenant-id:0}") Long assetUploadCleanupTenantId) {
        this(allowedOrigins,
                allowCredentials,
                eventReportSecret,
                jwtSecret,
                datasourceUsername,
                datasourcePassword,
                secretCipherKey,
                assetUploadWebhookSecret,
                assetUploadS3Enabled,
                assetUploadS3Endpoint,
                assetUploadS3Bucket,
                assetUploadS3AccessKey,
                assetUploadS3SecretKey,
                assetUploadS3PublicBaseUrl,
                assetUploadCleanupEnabled,
                assetUploadCleanupTenantId,
                MAX_ASSET_UPLOAD_WEBHOOK_TOLERANCE_SECONDS);
    }

    @Autowired
    public ProductionConfigGuard(
            @Value("${canvas.cors.allowed-origins:}") List<String> allowedOrigins,
            @Value("${canvas.cors.allow-credentials:true}") boolean allowCredentials,
            @Value("${canvas.events.report-secret:}") String eventReportSecret,
            @Value("${canvas.jwt.secret:}") String jwtSecret,
            @Value("${spring.datasource.username:}") String datasourceUsername,
            @Value("${spring.datasource.password:}") String datasourcePassword,
            @Value("${canvas.secret-cipher.key:}") String secretCipherKey,
            @Value("${canvas.marketing.content.asset-upload.webhook-secret:}") String assetUploadWebhookSecret,
            @Value("${canvas.marketing.content.asset-upload.s3.enabled:false}") boolean assetUploadS3Enabled,
            @Value("${canvas.marketing.content.asset-upload.s3.endpoint:}") String assetUploadS3Endpoint,
            @Value("${canvas.marketing.content.asset-upload.s3.bucket:}") String assetUploadS3Bucket,
            @Value("${canvas.marketing.content.asset-upload.s3.access-key:}") String assetUploadS3AccessKey,
            @Value("${canvas.marketing.content.asset-upload.s3.secret-key:}") String assetUploadS3SecretKey,
            @Value("${canvas.marketing.content.asset-upload.s3.public-base-url:}") String assetUploadS3PublicBaseUrl,
            @Value("${canvas.marketing.content.asset-upload.cleanup.enabled:false}") boolean assetUploadCleanupEnabled,
            @Value("${canvas.marketing.content.asset-upload.cleanup.tenant-id:0}") Long assetUploadCleanupTenantId,
            @Value("${canvas.marketing.content.asset-upload.webhook-tolerance-seconds:300}") Integer assetUploadWebhookToleranceSeconds) {
        this.allowedOrigins = allowedOrigins;
        this.allowCredentials = allowCredentials;
        this.eventReportSecret = eventReportSecret;
        this.jwtSecret = jwtSecret;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
        this.secretCipherKey = secretCipherKey;
        this.assetUploadWebhookSecret = assetUploadWebhookSecret;
        this.assetUploadS3Enabled = assetUploadS3Enabled;
        this.assetUploadS3Endpoint = assetUploadS3Endpoint;
        this.assetUploadS3Bucket = assetUploadS3Bucket;
        this.assetUploadS3AccessKey = assetUploadS3AccessKey;
        this.assetUploadS3SecretKey = assetUploadS3SecretKey;
        this.assetUploadS3PublicBaseUrl = assetUploadS3PublicBaseUrl;
        this.assetUploadCleanupEnabled = assetUploadCleanupEnabled;
        this.assetUploadCleanupTenantId = assetUploadCleanupTenantId;
        this.assetUploadWebhookToleranceSeconds = assetUploadWebhookToleranceSeconds;
    }

    @Override
    public void afterSingletonsInstantiated() {
        validate();
    }

    void validate() {
        if (allowCredentials && containsWildcardOrigin()) {
            throw new IllegalStateException("CORS wildcard is forbidden in prod when credentials are allowed");
        }
        if (isBlank(eventReportSecret) || DEFAULT_EVENT_REPORT_SECRET.equals(eventReportSecret)) {
            throw new IllegalStateException("event report secret must be configured and cannot use the default value");
        }
        if (isBlank(jwtSecret)) {
            throw new IllegalStateException("jwt secret must be configured in prod");
        }
        if ("root".equals(datasourceUsername) || "root".equals(datasourcePassword)) {
            throw new IllegalStateException("root database credentials are forbidden in prod");
        }
        if (secretCipherKey == null || secretCipherKey.isBlank()
                || DEFAULT_SECRET_CIPHER_KEY.equals(secretCipherKey)) {
            throw new IllegalStateException("secret cipher key must be configured and cannot use the default value");
        }
        if (isBlank(assetUploadWebhookSecret)
                || assetUploadWebhookSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("asset upload webhook secret must be configured and at least 32 bytes in prod");
        }
        if (assetUploadWebhookToleranceSeconds == null
                || assetUploadWebhookToleranceSeconds < 1
                || assetUploadWebhookToleranceSeconds > MAX_ASSET_UPLOAD_WEBHOOK_TOLERANCE_SECONDS) {
            throw new IllegalStateException(
                    "asset upload webhook tolerance must be between 1 and 300 seconds in prod");
        }
        if (assetUploadS3Enabled) {
            validateAssetUploadS3();
        }
        if (assetUploadCleanupEnabled && (assetUploadCleanupTenantId == null || assetUploadCleanupTenantId <= 0)) {
            throw new IllegalStateException("asset upload cleanup tenant id must be configured in prod when cleanup is enabled");
        }
    }

    private void validateAssetUploadS3() {
        if (!isHttpsUrl(assetUploadS3Endpoint)) {
            throw new IllegalStateException("asset upload S3 endpoint must be an HTTPS URL in prod");
        }
        if (isBlank(assetUploadS3Bucket)) {
            throw new IllegalStateException("asset upload S3 bucket must be configured in prod");
        }
        if (isBlank(assetUploadS3AccessKey)) {
            throw new IllegalStateException("asset upload S3 access key must be configured in prod");
        }
        if (isBlank(assetUploadS3SecretKey)
                || assetUploadS3SecretKey.getBytes(StandardCharsets.UTF_8).length < 16) {
            throw new IllegalStateException("asset upload S3 secret key must be configured in prod");
        }
        if (!isHttpsUrl(assetUploadS3PublicBaseUrl)) {
            throw new IllegalStateException("asset upload S3 public base URL must be an HTTPS URL in prod");
        }
    }

    private boolean containsWildcardOrigin() {
        if (allowedOrigins == null) {
            return false;
        }
        return allowedOrigins.stream()
                .filter(origin -> origin != null)
                .map(String::trim)
                .anyMatch("*"::equals);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isHttpsUrl(String value) {
        return value != null && value.trim().startsWith("https://");
    }
}
