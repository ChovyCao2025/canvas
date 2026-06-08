package org.chovy.canvas.domain.bi.storage;

/**
 * S3CompatibleBiFileStorage 编排 domain.bi.storage 场景的领域业务规则。
 */
public class S3CompatibleBiFileStorage implements BiFileStorage {

    public static final String PROVIDER = "S3";

    private final S3CompatibleBiStorageProperties properties;
    private final S3ObjectClient objectClient;

    /**
     * 创建 S3CompatibleBiFileStorage 实例并注入 domain.bi.storage 场景依赖。
     * @param properties 配置对象，用于控制运行参数和策略开关。
     * @param objectClient 依赖组件，用于完成数据访问或外部能力调用。
     */
    public S3CompatibleBiFileStorage(S3CompatibleBiStorageProperties properties, S3ObjectClient objectClient) {
        this.properties = properties;
        this.objectClient = objectClient;
    }

    /**
     * 返回 S3 兼容对象存储提供方标识。
     *
     * @return 固定值 {@code S3}，用于导出和附件记录识别存储后端
     */
    @Override
    public String provider() {
        return PROVIDER;
    }

    /**
     * 将 BI 文件写入 S3 兼容对象存储。
     *
     * @param storageKey 相对逻辑 key，会叠加配置的 keyPrefix 并校验路径穿越
     * @param bytes 文件内容；为空时写入空对象
     * @return 存储对象描述，包含 provider、逻辑 key、公开路径或 s3 URI 以及字节数
     */
    @Override
    public BiStoredFile write(String storageKey, byte[] bytes) {
        String logicalKey = validateStorageKey(storageKey);
        String objectKey = objectKey(logicalKey);
        byte[] payload = bytes == null ? new byte[0] : bytes;
        objectClient.putObject(new S3ObjectRequest(properties.bucket(), objectKey), payload);
        return new BiStoredFile(PROVIDER, logicalKey, publicPath(objectKey), (long) payload.length);
    }

    /**
     * 从 S3 兼容对象存储读取 BI 文件。
     *
     * @param storageKey 相对逻辑 key
     * @return 对象字节内容
     * @throws IllegalStateException 当对象不存在或后端返回异常状态时抛出
     */
    @Override
    public byte[] read(String storageKey) {
        String logicalKey = validateStorageKey(storageKey);
        byte[] bytes = objectClient.getObject(new S3ObjectRequest(properties.bucket(), objectKey(logicalKey)));
        if (bytes == null) {
            throw new IllegalStateException("BI S3 storage object is not available: " + logicalKey);
        }
        return bytes;
    }

    /**
     * 删除 S3 兼容对象存储中的 BI 文件。
     *
     * @param storageKey 相对逻辑 key
     * @return {@code true} 表示对象已删除，{@code false} 表示对象原本不存在
     */
    @Override
    public boolean delete(String storageKey) {
        String logicalKey = validateStorageKey(storageKey);
        return objectClient.deleteObject(new S3ObjectRequest(properties.bucket(), objectKey(logicalKey)));
    }

    /**
     * 将导出文件和订阅附件保留期同步为 bucket 生命周期规则。
     *
     * @param exportRetentionDays 导出对象保留天数，小于等于 0 时不生成对应规则
     * @param attachmentRetentionDays 订阅附件保留天数，小于等于 0 时不生成对应规则
     */
    public void applyLifecyclePolicy(int exportRetentionDays, int attachmentRetentionDays) {
        String lifecycleXml = lifecyclePolicyXml(exportRetentionDays, attachmentRetentionDays);
        if (!lifecycleXml.isBlank()) {
            objectClient.putBucketLifecycle(new S3BucketLifecycleRequest(properties.bucket()), lifecycleXml);
        }
    }

    /**
     * 生成 S3 Lifecycle XML，分别约束 exports/ 和 attachments/ 前缀。
     */
    String lifecyclePolicyXml(int exportRetentionDays, int attachmentRetentionDays) {
        StringBuilder rules = new StringBuilder();
        appendLifecycleRule(
                rules,
                "canvas-bi-exports-retention",
                properties.keyPrefix() + "exports/",
                exportRetentionDays);
        appendLifecycleRule(
                rules,
                "canvas-bi-attachments-retention",
                properties.keyPrefix() + "attachments/",
                attachmentRetentionDays);
        if (rules.isEmpty()) {
            return "";
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<LifecycleConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
                + rules
                + "</LifecycleConfiguration>";
    }

    /**
     * 追加单个对象前缀的过期规则。
     *
     * <p>保留天数小于等于 0 时跳过规则生成，避免把关闭保留策略误写成立即过期。</p>
     */
    private void appendLifecycleRule(StringBuilder rules, String id, String prefix, int expirationDays) {
        if (expirationDays <= 0) {
            return;
        }
        rules.append("<Rule>")
                .append("<ID>").append(xml(id)).append("</ID>")
                .append("<Filter><Prefix>").append(xml(prefix)).append("</Prefix></Filter>")
                .append("<Status>Enabled</Status>")
                .append("<Expiration><Days>").append(expirationDays).append("</Days></Expiration>")
                .append("</Rule>");
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param storageKey 业务键，用于在同一租户下定位资源。
     * @return 返回 object key 生成的文本或业务键。
     */
    private String objectKey(String storageKey) {
        return properties.keyPrefix() + storageKey;
    }

    /**
     * 执行 publicPath 流程，围绕 public path 完成校验、计算或结果组装。
     *
     * @param objectKey 业务键，用于在同一租户下定位资源。
     * @return 返回 public path 生成的文本或业务键。
     */
    private String publicPath(String objectKey) {
        if (properties.publicBaseUrl() != null && !properties.publicBaseUrl().isBlank()) {
            String baseUrl = properties.publicBaseUrl();
            while (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            return baseUrl + "/" + objectKey;
        }
        return "s3://" + properties.bucket() + "/" + objectKey;
    }

    /**
     * 校验逻辑 key 为相对路径，防止对象前缀被调用方逃逸。
     */
    private String validateStorageKey(String storageKey) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("storageKey is required");
        }
        String normalized = storageKey.trim().replace('\\', '/');
        if (normalized.startsWith("/")) {
            throw new IllegalArgumentException("storageKey must be relative: " + storageKey);
        }
        if (normalized.equals("..")
                || normalized.startsWith("../")
                || normalized.endsWith("/..")
                || normalized.contains("/../")) {
            throw new IllegalArgumentException("storageKey must not contain traversal: " + storageKey);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return normalized;
    }

    /**
     * 执行 xml 流程，围绕 xml 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 xml 生成的文本或业务键。
     */
    private String xml(String value) {
        return (value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
