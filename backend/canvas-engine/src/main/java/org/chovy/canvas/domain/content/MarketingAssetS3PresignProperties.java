package org.chovy.canvas.domain.content;

public record MarketingAssetS3PresignProperties(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        String keyPrefix,
        boolean pathStyle,
        String publicBaseUrl
) {

    public MarketingAssetS3PresignProperties {
        endpoint = required(endpoint, "endpoint");
        region = required(region, "region");
        bucket = required(bucket, "bucket");
        accessKey = required(accessKey, "accessKey");
        secretKey = required(secretKey, "secretKey");
        keyPrefix = normalizePrefix(keyPrefix);
        publicBaseUrl = required(publicBaseUrl, "publicBaseUrl");
    }

    String objectKey(String relativeObjectKey) {
        String normalized = normalizeRelativeKey(relativeObjectKey);
        return keyPrefix + normalized;
    }

    String publicUrl(String relativeObjectKey) {
        String base = publicBaseUrl;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + objectKey(relativeObjectKey);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("canvas.marketing.content.asset-upload.s3." + field + " is required");
        }
        return value.trim();
    }

    private static String normalizePrefix(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = normalizeRelativeKey(value);
        return normalized.isBlank() ? "" : normalized + "/";
    }

    private static String normalizeRelativeKey(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("S3 object key is required");
        }
        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.contains("../") || normalized.contains("/..") || normalized.equals("..")) {
            throw new IllegalArgumentException("S3 object key must not contain parent directory segments");
        }
        return normalized;
    }
}
