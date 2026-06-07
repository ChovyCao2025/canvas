package org.chovy.canvas.domain.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MarketingMonitorProviderCredentialDO;
import org.chovy.canvas.dal.mapper.MarketingMonitorProviderCredentialMapper;
import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.chovy.canvas.domain.providerwrite.ProviderWriteSandboxSupport;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
public class SearchMarketingCredentialResolver {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final MarketingMonitorProviderCredentialMapper credentialMapper;
    private final SecretCipher secretCipher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public SearchMarketingCredentialResolver(MarketingMonitorProviderCredentialMapper credentialMapper,
                                             SecretCipher secretCipher,
                                             ObjectMapper objectMapper) {
        this(credentialMapper, secretCipher, objectMapper, Clock.systemDefaultZone());
    }

    SearchMarketingCredentialResolver(MarketingMonitorProviderCredentialMapper credentialMapper,
                                      SecretCipher secretCipher,
                                      ObjectMapper objectMapper,
                                      Clock clock) {
        this.credentialMapper = credentialMapper;
        this.secretCipher = secretCipher;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public SearchMarketingCredentialRef resolve(Long tenantId, String provider, String credentialKey) {
        String normalizedProvider = normalize(provider);
        if (ProviderWriteSandboxSupport.supportsSandboxProvider(normalizedProvider)) {
            return SearchMarketingCredentialRef.sandbox();
        }
        String normalizedKey = normalizeKey(credentialKey);
        if (normalizedKey == null) {
            return SearchMarketingCredentialRef.unavailable(null, normalizedProvider,
                    "SEARCH_PROVIDER_CREDENTIAL_KEY_REQUIRED",
                    "search provider credential key is required");
        }
        MarketingMonitorProviderCredentialDO row = credentialMapper.selectOne(
                new LambdaQueryWrapper<MarketingMonitorProviderCredentialDO>()
                        .eq(MarketingMonitorProviderCredentialDO::getTenantId, normalizeTenant(tenantId))
                        .eq(MarketingMonitorProviderCredentialDO::getCredentialKey, normalizedKey)
                        .eq(MarketingMonitorProviderCredentialDO::getProviderType, normalizedProvider)
                        .last("LIMIT 1"));
        if (row == null || !normalizeTenant(tenantId).equals(row.getTenantId())) {
            return SearchMarketingCredentialRef.unavailable(normalizedKey, normalizedProvider,
                    "SEARCH_PROVIDER_CREDENTIAL_NOT_FOUND",
                    "search provider credential is not found");
        }
        if (!"ACTIVE".equals(normalize(row.getStatus()))) {
            return SearchMarketingCredentialRef.unavailable(normalizedKey, normalizedProvider,
                    "SEARCH_PROVIDER_CREDENTIAL_DISABLED",
                    "search provider credential is disabled");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (row.getExpiresAt() != null && !row.getExpiresAt().isAfter(now)) {
            return SearchMarketingCredentialRef.unavailable(normalizedKey, normalizedProvider,
                    "SEARCH_PROVIDER_CREDENTIAL_EXPIRED",
                    "search provider credential is expired");
        }
        String accessToken = decrypt(row.getAccessTokenCiphertext());
        String developerToken = decrypt(row.getApiKeyCiphertext());
        if (accessToken == null && developerToken == null) {
            return SearchMarketingCredentialRef.unavailable(normalizedKey, normalizedProvider,
                    "SEARCH_PROVIDER_CREDENTIAL_SECRET_MISSING",
                    "search provider credential secret is missing");
        }
        return new SearchMarketingCredentialRef(
                row.getId(),
                row.getCredentialKey(),
                row.getProviderType(),
                row.getAuthType(),
                accessToken,
                developerToken,
                decrypt(row.getRefreshTokenCiphertext()),
                row.getExpiresAt(),
                safeMetadata(row.getMetadataJson()),
                true,
                null,
                null);
    }

    private Map<String, Object> safeMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return ProviderWriteEvidenceSanitizer.sanitizeMap(objectMapper.readValue(metadataJson, OBJECT_MAP));
        } catch (JsonProcessingException ex) {
            return Map.of("metadataParseError", "invalid credential metadata JSON");
        }
    }

    private String decrypt(String ciphertext) {
        String value = secretCipher.decrypt(ciphertext);
        return value == null || value.isBlank() ? null : value;
    }

    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
