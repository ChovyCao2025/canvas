package org.chovy.canvas.domain.bi.storage;

public record S3CompatibleBiStorageProperties(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        String keyPrefix,
        boolean pathStyle,
        String publicBaseUrl,
        int connectTimeoutMs,
        int requestTimeoutMs) {

    public S3CompatibleBiStorageProperties {
        endpoint = required(endpoint, "endpoint");
        region = required(region, "region");
        bucket = required(bucket, "bucket");
        accessKey = required(accessKey, "accessKey");
        secretKey = required(secretKey, "secretKey");
        keyPrefix = normalizePrefix(keyPrefix);
        publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        connectTimeoutMs = connectTimeoutMs <= 0 ? 3000 : connectTimeoutMs;
        requestTimeoutMs = requestTimeoutMs <= 0 ? 15000 : requestTimeoutMs;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("canvas.bi.storage.s3." + field + " is required");
        }
        return value.trim();
    }

    private static String normalizePrefix(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? "" : normalized + "/";
    }
}
