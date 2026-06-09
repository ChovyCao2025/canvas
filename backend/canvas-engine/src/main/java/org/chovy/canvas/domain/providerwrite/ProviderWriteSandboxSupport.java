package org.chovy.canvas.domain.providerwrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ProviderWriteSandboxSupport 编排 domain.providerwrite 场景的领域业务规则。
 */
public final class ProviderWriteSandboxSupport {

    /**
     * 执行 ProviderWriteSandboxSupport 流程，围绕 provider write sandbox support 完成校验、计算或结果组装。
     */
    private ProviderWriteSandboxSupport() {
    }

    /**
     * supportsSandboxProvider 处理 domain.providerwrite 场景的业务逻辑。
     * @param provider provider 参数，用于 supportsSandboxProvider 流程中的校验、计算或对象转换。
     * @return 返回 supports sandbox provider 的布尔判断结果。
     */
    public static boolean supportsSandboxProvider(String provider) {
        String normalized = normalize(provider);
        return "SANDBOX".equals(normalized)
                || normalized.endsWith("_SANDBOX")
                || normalized.startsWith("SANDBOX_");
    }

    /**
     * operationId 处理 domain.providerwrite 场景的业务逻辑。
     * @param domain domain 参数，用于 operationId 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 operationId 流程中的校验、计算或对象转换。
     * @param mutationType 类型标识，用于选择对应处理分支。
     * @param idempotencyKey 业务键，用于在同一租户下定位资源。
     * @param dryRun dry run 参数，用于 operationId 流程中的校验、计算或对象转换。
     * @return 返回 operation id 生成的文本或业务键。
     */
    public static String operationId(String domain,
                                     String provider,
                                     String mutationType,
                                     String idempotencyKey,
                                     boolean dryRun) {
        String material = String.join("|",
                defaultString(domain),
                normalize(provider),
                normalize(mutationType),
                defaultString(idempotencyKey),
                dryRun ? "dry-run" : "apply");
        return "sandbox-" + defaultString(domain) + "-" + (dryRun ? "validate-" : "apply-")
                /**
                 * 执行 sha256 流程，围绕 sha256 完成校验、计算或结果组装。
                 *
                 * @return 返回 sha256 流程生成的业务结果。
                 */
                + sha256(material).substring(0, 16);
    }

    /**
     * response 处理 domain.providerwrite 场景的业务逻辑。
     * @param domain domain 参数，用于 response 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 response 流程中的校验、计算或对象转换。
     * @param mutationType 类型标识，用于选择对应处理分支。
     * @param entityType 类型标识，用于选择对应处理分支。
     * @param externalEntityId 业务对象 ID，用于定位具体记录。
     * @param idempotencyKey 业务键，用于在同一租户下定位资源。
     * @param dryRun dry run 参数，用于 response 流程中的校验、计算或对象转换。
     * @param partialFailure partial failure 参数，用于 response 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param metadata metadata 参数，用于 response 流程中的校验、计算或对象转换。
     * @return 返回 response 流程生成的业务结果。
     */
    public static Map<String, Object> response(String domain,
                                               String provider,
                                               String mutationType,
                                               String entityType,
                                               String externalEntityId,
                                               String idempotencyKey,
                                               boolean dryRun,
                                               boolean partialFailure,
                                               Map<String, Object> payload,
                                               Map<String, Object> metadata) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("adapter", "sandbox");
        response.put("domain", domain);
        response.put("provider", provider);
        response.put("mutationType", mutationType);
        response.put("entityType", entityType);
        response.put("externalEntityId", defaultString(externalEntityId));
        response.put("idempotencyKey", defaultString(idempotencyKey));
        response.put("dryRun", dryRun);
        response.put("partialFailure", partialFailure);
        response.put("validated", true);
        response.put("applied", !dryRun);
        response.put("payloadHash", sha256(String.valueOf(payload == null ? Map.of() : payload)));
        response.put("metadata", ProviderWriteEvidenceSanitizer.sanitizeMap(metadata));
        response.put("access_token", "sandbox-token-never-persist");
        return ProviderWriteEvidenceSanitizer.sanitizeMap(response);
    }

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalize(String value) {
        return defaultString(value).toUpperCase(Locale.ROOT);
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private static String defaultString(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 执行 sha256 流程，围绕 sha256 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sha256 生成的文本或业务键。
     */
    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash provider write sandbox evidence", ex);
        }
    }
}
