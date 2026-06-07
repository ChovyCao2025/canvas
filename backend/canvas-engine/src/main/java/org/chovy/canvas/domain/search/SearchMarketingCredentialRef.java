package org.chovy.canvas.domain.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public static SearchMarketingCredentialRef sandbox() {
        return new SearchMarketingCredentialRef(null, "SANDBOX_SEARCH", "SANDBOX_SEARCH", "SANDBOX",
                null, null, null, null, Map.of("mode", "sandbox"), true, null, null);
    }

    public static SearchMarketingCredentialRef unavailable(String credentialKey,
                                                           String providerType,
                                                           String errorCode,
                                                           String errorMessage) {
        return new SearchMarketingCredentialRef(null, credentialKey, providerType, null,
                null, null, null, null, Map.of(), false, errorCode, errorMessage);
    }

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

    @Override
    public String toString() {
        return "SearchMarketingCredentialRef" + safeEvidence();
    }
}
