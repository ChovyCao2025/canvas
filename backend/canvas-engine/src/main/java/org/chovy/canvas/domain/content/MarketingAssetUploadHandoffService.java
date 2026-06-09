package org.chovy.canvas.domain.content;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MarketingAssetUploadHandoffService 编排 domain.content 场景的领域业务规则。
 */
@Service
public class MarketingAssetUploadHandoffService {

    private final boolean s3Enabled;
    private final MarketingAssetS3Presigner s3Presigner;
    private final String cloudinaryDirectUploadUrl;
    private final String muxDirectUploadUrl;
    private final String externalRegisterUrl;

    /**
     * 创建 MarketingAssetUploadHandoffService 实例并注入 domain.content 场景依赖。
     * @param s3Enabled s3 enabled 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param s3Endpoint s3 endpoint 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param s3Region s3 region 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param s3Bucket s3 bucket 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param s3AccessKey 业务键，用于在同一租户下定位资源。
     * @param s3SecretKey 业务键，用于在同一租户下定位资源。
     * @param s3KeyPrefix s3 key prefix 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param s3PathStyle s3 path style 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param s3PublicBaseUrl s3 public base url 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param cloudinaryDirectUploadUrl cloudinary direct upload url 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param muxDirectUploadUrl mux direct upload url 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param externalRegisterUrl external register url 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public MarketingAssetUploadHandoffService(
            @Value("${canvas.marketing.content.asset-upload.s3.enabled:false}") boolean s3Enabled,
            @Value("${canvas.marketing.content.asset-upload.s3.endpoint:}") String s3Endpoint,
            @Value("${canvas.marketing.content.asset-upload.s3.region:us-east-1}") String s3Region,
            @Value("${canvas.marketing.content.asset-upload.s3.bucket:}") String s3Bucket,
            @Value("${canvas.marketing.content.asset-upload.s3.access-key:}") String s3AccessKey,
            @Value("${canvas.marketing.content.asset-upload.s3.secret-key:}") String s3SecretKey,
            @Value("${canvas.marketing.content.asset-upload.s3.key-prefix:}") String s3KeyPrefix,
            @Value("${canvas.marketing.content.asset-upload.s3.path-style:true}") boolean s3PathStyle,
            @Value("${canvas.marketing.content.asset-upload.s3.public-base-url:}") String s3PublicBaseUrl,
            @Value("${canvas.marketing.content.asset-upload.cloudinary.direct-upload-url:/provider/cloudinary/direct-upload}") String cloudinaryDirectUploadUrl,
            @Value("${canvas.marketing.content.asset-upload.mux.direct-upload-url:/provider/mux/direct-upload}") String muxDirectUploadUrl,
            @Value("${canvas.marketing.content.asset-upload.external.register-url:/provider/external/register}") String externalRegisterUrl) {
        this(s3Enabled, s3Endpoint, s3Region, s3Bucket, s3AccessKey, s3SecretKey, s3KeyPrefix, s3PathStyle,
                s3PublicBaseUrl, cloudinaryDirectUploadUrl, muxDirectUploadUrl, externalRegisterUrl, Clock.systemUTC());
    }

    /**
     * 执行 MarketingAssetUploadHandoffService 流程，围绕 marketing asset upload handoff service 完成校验、计算或结果组装。
     *
     * @param s3Enabled s3 enabled 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param s3Endpoint s3 endpoint 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param s3Region s3 region 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param s3Bucket s3 bucket 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param s3AccessKey 业务键，用于在同一租户下定位资源。
     * @param s3SecretKey 业务键，用于在同一租户下定位资源。
     * @param s3KeyPrefix s3 key prefix 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param s3PathStyle s3 path style 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param s3PublicBaseUrl s3 public base url 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param cloudinaryDirectUploadUrl cloudinary direct upload url 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param muxDirectUploadUrl mux direct upload url 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param externalRegisterUrl external register url 参数，用于 MarketingAssetUploadHandoffService 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    MarketingAssetUploadHandoffService(boolean s3Enabled,
                                       String s3Endpoint,
                                       String s3Region,
                                       String s3Bucket,
                                       String s3AccessKey,
                                       String s3SecretKey,
                                       String s3KeyPrefix,
                                       boolean s3PathStyle,
                                       String s3PublicBaseUrl,
                                       String cloudinaryDirectUploadUrl,
                                       String muxDirectUploadUrl,
                                       String externalRegisterUrl,
                                       Clock clock) {
        this.s3Enabled = s3Enabled;
        this.s3Presigner = s3Enabled
                /**
                 * 处理安全、签名或敏感信息逻辑。
                 *
                 * @param clock 时间参数，用于计算窗口、过期或审计时间。
                 * @return 返回 MarketingAssetS3Presigner 流程生成的业务结果。
                 */
                ? new MarketingAssetS3Presigner(new MarketingAssetS3PresignProperties(
                s3Endpoint,
                s3Region,
                s3Bucket,
                s3AccessKey,
                s3SecretKey,
                s3KeyPrefix,
                s3PathStyle,
                s3PublicBaseUrl), clock)
                : null;
        this.cloudinaryDirectUploadUrl = defaultString(cloudinaryDirectUploadUrl, "/provider/cloudinary/direct-upload");
        this.muxDirectUploadUrl = defaultString(muxDirectUploadUrl, "/provider/mux/direct-upload");
        this.externalRegisterUrl = defaultString(externalRegisterUrl, "/provider/external/register");
    }

    /**
     * 执行 contractOnly 流程，围绕 contract only 完成校验、计算或结果组装。
     *
     * @return 返回 contractOnly 流程生成的业务结果。
     */
    static MarketingAssetUploadHandoffService contractOnly() {
        return new MarketingAssetUploadHandoffService(false, "", "us-east-1", "", "", "", "", true, "",
                "/provider/cloudinary/direct-upload",
                "/provider/mux/direct-upload",
                "/provider/external/register",
                Clock.systemUTC());
    }

    /**
     * 创建业务记录，作为营销内容的服务入口。
     * <p>该方法不接收显式租户参数时，会依赖输入对象、密钥或已有记录携带的租户信息维持隔离。
     * @param request 本次操作的业务请求参数，包含目标对象、状态或外部回调载荷
     * @return 返回本次业务操作的结果对象
     */
    public MarketingAssetUploadHandoff create(MarketingAssetUploadHandoffRequest request) {
        String provider = request.provider();
        if ("S3".equals(provider) && s3Enabled) {
            return s3Handoff(request);
        }
        return contractHandoff(request);
    }

    /**
     * 执行 s3Handoff 流程，围绕 s3 handoff 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 s3Handoff 流程生成的业务结果。
     */
    private MarketingAssetUploadHandoff s3Handoff(MarketingAssetUploadHandoffRequest request) {
        MarketingAssetS3Presigner.PresignedPut presigned = s3Presigner.presignPut(
                request.objectKey(),
                request.mimeType(),
                request.requiredHeaders(),
                request.ttl());
        Map<String, Object> params = baseParams(request);
        params.put("handoffMode", "PRESIGNED_PUT");
        params.put("storageProvider", "S3");
        params.put("objectKey", presigned.objectKey());
        params.put("storageUrl", presigned.storageUrl());
        params.put("signedAt", presigned.signedAt());
        params.put("expiresInSeconds", presigned.expiresInSeconds());
        params.put("checksumRequiredAtCallback", true);
        return new MarketingAssetUploadHandoff(presigned.uploadUrl(), presigned.storageUrl(), params,
                presigned.requiredHeaders());
    }

    /**
     * 执行 contractHandoff 流程，围绕 contract handoff 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 contractHandoff 流程生成的业务结果。
     */
    private MarketingAssetUploadHandoff contractHandoff(MarketingAssetUploadHandoffRequest request) {
        Map<String, Object> params = baseParams(request);
        params.put("handoffMode", "PROVIDER_CONTRACT");
        params.put("objectKey", request.objectKey());
        params.put("callbackEndpointRequired", true);
        String uploadUrl = switch (request.provider()) {
            case "CLOUDINARY" -> cloudinaryDirectUploadUrl;
            case "MUX" -> muxDirectUploadUrl;
            case "EXTERNAL" -> externalRegisterUrl;
            default -> "/provider/s3/presigned-put";
        };
        return new MarketingAssetUploadHandoff(uploadUrl, null, params, request.requiredHeaders());
    }

    /**
     * 执行 baseParams 流程，围绕 base params 完成校验、计算或结果组装。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 baseParams 流程生成的业务结果。
     */
    private Map<String, Object> baseParams(MarketingAssetUploadHandoffRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("provider", request.provider());
        params.put("assetKey", request.assetKey());
        params.put("assetType", request.assetType());
        params.put("mimeType", request.mimeType());
        params.put("fileName", MarketingContentSupport.trimToNull(request.fileName()));
        params.put("sizeBytes", request.sizeBytes());
        params.put("uploadToken", request.uploadToken());
        return params;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
