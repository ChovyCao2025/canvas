package org.chovy.canvas.config;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * ProductionConfigGuard 提供 config 场景的 Spring 配置或启动校验。
 */
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

    /**
     * 创建 ProductionConfigGuard 实例并注入 config 场景依赖。
     * @param allowedOrigins allowed origins 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param allowCredentials allow credentials 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param eventReportSecret event report secret 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param jwtSecret jwt secret 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param datasourceUsername 名称文本，用于展示或唯一性校验。
     * @param datasourcePassword datasource password 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param secretCipherKey 业务键，用于在同一租户下定位资源。
     * @param assetUploadWebhookSecret asset upload webhook secret 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param assetUploadS3Enabled asset upload s3 enabled 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param assetUploadS3Endpoint asset upload s3 endpoint 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param assetUploadS3Bucket asset upload s3 bucket 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param assetUploadS3AccessKey 业务键，用于在同一租户下定位资源。
     * @param assetUploadS3SecretKey 业务键，用于在同一租户下定位资源。
     * @param assetUploadS3PublicBaseUrl asset upload s3 public base url 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param assetUploadCleanupEnabled asset upload cleanup enabled 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param assetUploadCleanupTenantId 业务对象 ID，用于定位具体记录。
     */
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

    /**
     * 创建 ProductionConfigGuard 实例并注入 config 场景依赖。
     * @param allowedOrigins allowed origins 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param allowCredentials allow credentials 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param eventReportSecret event report secret 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param jwtSecret jwt secret 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param datasourceUsername 名称文本，用于展示或唯一性校验。
     * @param datasourcePassword datasource password 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param secretCipherKey 业务键，用于在同一租户下定位资源。
     * @param assetUploadWebhookSecret asset upload webhook secret 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param assetUploadS3Enabled asset upload s3 enabled 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param assetUploadS3Endpoint asset upload s3 endpoint 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param assetUploadS3Bucket asset upload s3 bucket 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param assetUploadS3AccessKey 业务键，用于在同一租户下定位资源。
     * @param assetUploadS3SecretKey 业务键，用于在同一租户下定位资源。
     * @param assetUploadS3PublicBaseUrl asset upload s3 public base url 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param assetUploadCleanupEnabled asset upload cleanup enabled 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     * @param assetUploadCleanupTenantId 业务对象 ID，用于定位具体记录。
     * @param assetUploadWebhookToleranceSeconds asset upload webhook tolerance seconds 参数，用于 ProductionConfigGuard 流程中的校验、计算或对象转换。
     */
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

    /**
     * afterSingletonsInstantiated 处理 config 场景的业务逻辑。
     */
    @Override
    public void afterSingletonsInstantiated() {
        // 所有配置 Bean 绑定后立即校验，避免生产环境带着开发默认值启动。
        validate();
    }

    /**
     * 校验输入、权限或业务前置条件。
     */
    void validate() {
        // 携带凭证的 CORS 若允许通配来源，会把认证接口暴露给任意站点。
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
        // 清理任务按租户隔离；缺失租户会把保留策略误变成全局操作。
        if (assetUploadCleanupEnabled && (assetUploadCleanupTenantId == null || assetUploadCleanupTenantId <= 0)) {
            throw new IllegalStateException("asset upload cleanup tenant id must be configured in prod when cleanup is enabled");
        }
    }

    /**
     * 校验生产环境资产上传 S3 配置完整且使用 HTTPS 地址。
     */
    private void validateAssetUploadS3() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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

    /**
     * 判断 CORS 来源配置是否包含通配来源。
     *
     * @return true 表示包含 *
     */
    private boolean containsWildcardOrigin() {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (allowedOrigins == null) {
            return false;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return allowedOrigins.stream()
                .filter(origin -> origin != null)
                .map(String::trim)
                .anyMatch("*"::equals);
    }

    /**
     * 判断字符串是否为空。
     *
     * @param value 待检查字符串
     * @return true 表示为空或全空白
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 判断配置值是否为 HTTPS URL。
     *
     * @param value URL 配置值
     * @return true 表示以 https:// 开头
     */
    private boolean isHttpsUrl(String value) {
        return value != null && value.trim().startsWith("https://");
    }
}
