package org.chovy.canvas.domain.bi.storage;

/**
 * S3CompatibleBiStorageProperties 承载 domain.bi.storage 场景中的不可变数据快照。
 * @param endpoint endpoint 字段。
 * @param region region 字段。
 * @param bucket bucket 字段。
 * @param accessKey accessKey 字段。
 * @param secretKey secretKey 字段。
 * @param keyPrefix keyPrefix 字段。
 * @param pathStyle pathStyle 字段。
 * @param publicBaseUrl publicBaseUrl 字段。
 * @param connectTimeoutMs connectTimeoutMs 字段。
 * @param requestTimeoutMs requestTimeoutMs 字段。
 */
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

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("canvas.bi.storage.s3." + field + " is required");
        }
        return value.trim();
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizePrefix(String value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().replace('\\', '/');
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return normalized.isBlank() ? "" : normalized + "/";
    }
}
