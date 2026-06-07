package org.chovy.canvas.domain.content;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MarketingAssetUploadHandoffService {

    private final boolean s3Enabled;
    private final MarketingAssetS3Presigner s3Presigner;
    private final String cloudinaryDirectUploadUrl;
    private final String muxDirectUploadUrl;
    private final String externalRegisterUrl;

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

    static MarketingAssetUploadHandoffService contractOnly() {
        return new MarketingAssetUploadHandoffService(false, "", "us-east-1", "", "", "", "", true, "",
                "/provider/cloudinary/direct-upload",
                "/provider/mux/direct-upload",
                "/provider/external/register",
                Clock.systemUTC());
    }

    public MarketingAssetUploadHandoff create(MarketingAssetUploadHandoffRequest request) {
        String provider = request.provider();
        if ("S3".equals(provider) && s3Enabled) {
            return s3Handoff(request);
        }
        return contractHandoff(request);
    }

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

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
