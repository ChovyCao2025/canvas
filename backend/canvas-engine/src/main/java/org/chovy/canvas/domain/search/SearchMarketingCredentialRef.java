package org.chovy.canvas.domain.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SearchMarketingCredentialRef 承载 domain.search 场景中的不可变数据快照。
 * @param credentialId credentialId 字段。
 * @param credentialKey credentialKey 字段。
 * @param providerType providerType 字段。
 * @param authType authType 字段。
 * @param accessToken accessToken 字段。
 * @param developerToken developerToken 字段。
 * @param refreshToken refreshToken 字段。
 * @param expiresAt expiresAt 字段。
 * @param safeMetadata safeMetadata 字段。
 * @param available available 字段。
 * @param errorCode errorCode 字段。
 * @param errorMessage errorMessage 字段。
 */
public record SearchMarketingCredentialRef(
        Long credentialId,
        String credentialKey,
        String providerType,
        String authType,
        @JsonIgnore String accessToken,
        @JsonIgnore String developerToken,
        @JsonIgnore String refreshToken,
        LocalDateTime expiresAt,
        Map<String, Object> safeMetadata,
        boolean available,
        String errorCode,
        String errorMessage) {

    public SearchMarketingCredentialRef {
        safeMetadata = ProviderWriteEvidenceSanitizer.sanitizeMap(safeMetadata);
    }

    /**
     * sandbox 处理 domain.search 场景的业务逻辑。
     * @return 返回 sandbox 流程生成的业务结果。
     */
    public static SearchMarketingCredentialRef sandbox() {
        return new SearchMarketingCredentialRef(null, "SANDBOX_SEARCH", "SANDBOX_SEARCH", "SANDBOX",
                null, null, null, null, Map.of("mode", "sandbox"), true, null, null);
    }

    /**
     * unavailable 处理 domain.search 场景的业务逻辑。
     * @param credentialKey 业务键，用于在同一租户下定位资源。
     * @param providerType 类型标识，用于选择对应处理分支。
     * @param errorCode 业务编码，用于匹配对应类型或状态。
     * @param errorMessage error message 参数，用于 unavailable 流程中的校验、计算或对象转换。
     * @return 返回 unavailable 流程生成的业务结果。
     */
    public static SearchMarketingCredentialRef unavailable(String credentialKey,
                                                           String providerType,
                                                           String errorCode,
                                                           String errorMessage) {
        return new SearchMarketingCredentialRef(null, credentialKey, providerType, null,
                null, null, null, null, Map.of(), false, errorCode, errorMessage);
    }

    /**
     * safeEvidence 处理 domain.search 场景的业务逻辑。
     * @return 返回 safeEvidence 流程生成的业务结果。
     */
    public Map<String, Object> safeEvidence() {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("credentialId", credentialId);
        evidence.put("credentialKey", credentialKey);
        evidence.put("providerType", providerType);
        evidence.put("authType", authType);
        evidence.put("expiresAt", expiresAt == null ? null : expiresAt.toString());
        evidence.put("available", available);
        evidence.put("errorCode", errorCode);
        evidence.put("metadata", safeMetadata);
        return ProviderWriteEvidenceSanitizer.sanitizeMap(evidence);
    }

    /**
     * toString 校验或转换 domain.search 场景的数据。
     * @return 返回组装或转换后的结果对象。
     */
    @Override
    public String toString() {
        return "SearchMarketingCredentialRef" + safeEvidence();
    }
}
