package org.chovy.canvas.domain.bi.storage;

public class S3CompatibleBiFileStorage implements BiFileStorage {

    public static final String PROVIDER = "S3";

    private final S3CompatibleBiStorageProperties properties;
    private final S3ObjectClient objectClient;

    public S3CompatibleBiFileStorage(S3CompatibleBiStorageProperties properties, S3ObjectClient objectClient) {
        this.properties = properties;
        this.objectClient = objectClient;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public BiStoredFile write(String storageKey, byte[] bytes) {
        String logicalKey = validateStorageKey(storageKey);
        String objectKey = objectKey(logicalKey);
        byte[] payload = bytes == null ? new byte[0] : bytes;
        objectClient.putObject(new S3ObjectRequest(properties.bucket(), objectKey), payload);
        return new BiStoredFile(PROVIDER, logicalKey, publicPath(objectKey), (long) payload.length);
    }

    @Override
    public byte[] read(String storageKey) {
        String logicalKey = validateStorageKey(storageKey);
        byte[] bytes = objectClient.getObject(new S3ObjectRequest(properties.bucket(), objectKey(logicalKey)));
        if (bytes == null) {
            throw new IllegalStateException("BI S3 storage object is not available: " + logicalKey);
        }
        return bytes;
    }

    @Override
    public boolean delete(String storageKey) {
        String logicalKey = validateStorageKey(storageKey);
        return objectClient.deleteObject(new S3ObjectRequest(properties.bucket(), objectKey(logicalKey)));
    }

    public void applyLifecyclePolicy(int exportRetentionDays, int attachmentRetentionDays) {
        String lifecycleXml = lifecyclePolicyXml(exportRetentionDays, attachmentRetentionDays);
        if (!lifecycleXml.isBlank()) {
            objectClient.putBucketLifecycle(new S3BucketLifecycleRequest(properties.bucket()), lifecycleXml);
        }
    }

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

    private String objectKey(String storageKey) {
        return properties.keyPrefix() + storageKey;
    }

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

    private String validateStorageKey(String storageKey) {
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
        return normalized;
    }

    private String xml(String value) {
        return (value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
