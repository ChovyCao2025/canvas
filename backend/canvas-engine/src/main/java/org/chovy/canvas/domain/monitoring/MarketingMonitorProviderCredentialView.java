package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record MarketingMonitorProviderCredentialView(
        Long id,
        Long tenantId,
        String credentialKey,
        String providerType,
        String authType,
        String displayName,
        String status,
        String tokenType,
        List<String> scopes,
        String accessTokenPrefix,
        String refreshTokenPrefix,
        String apiKeyPrefix,
        String refreshEndpoint,
        String revokeEndpoint,
        LocalDateTime expiresAt,
        LocalDateTime refreshTokenExpiresAt,
        LocalDateTime revokedAt,
        LocalDateTime lastRefreshedAt,
        int refreshAttemptCount,
        String lastRefreshStatus,
        String lastRefreshError,
        String lastRevokeStatus,
        String lastRevokeError,
        Map<String, Object> metadata,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
