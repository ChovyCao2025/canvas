package org.chovy.canvas.domain.content;

/**
 * MarketingAssetS3PresignProperties 承载 domain.content 场景中的不可变数据快照。
 * @param endpoint endpoint 字段。
 * @param region region 字段。
 * @param bucket bucket 字段。
 * @param accessKey accessKey 字段。
 * @param secretKey secretKey 字段。
 * @param keyPrefix keyPrefix 字段。
 * @param pathStyle pathStyle 字段。
 * @param publicBaseUrl publicBaseUrl 字段。
 */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param relativeObjectKey 业务键，用于在同一租户下定位资源。
     * @return 返回 object key 生成的文本或业务键。
     */
    String objectKey(String relativeObjectKey) {
        String normalized = normalizeRelativeKey(relativeObjectKey);
        return keyPrefix + normalized;
    }

    /**
     * 执行 publicUrl 流程，围绕 public url 完成校验、计算或结果组装。
     *
     * @param relativeObjectKey 业务键，用于在同一租户下定位资源。
     * @return 返回 public url 生成的文本或业务键。
     */
    String publicUrl(String relativeObjectKey) {
        String base = publicBaseUrl;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + objectKey(relativeObjectKey);
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
            throw new IllegalArgumentException("canvas.marketing.content.asset-upload.s3." + field + " is required");
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
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = normalizeRelativeKey(value);
        return normalized.isBlank() ? "" : normalized + "/";
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeRelativeKey(String value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("S3 object key is required");
        }
        String normalized = value.trim().replace('\\', '/');
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.contains("../") || normalized.contains("/..") || normalized.equals("..")) {
            throw new IllegalArgumentException("S3 object key must not contain parent directory segments");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return normalized;
    }
}
