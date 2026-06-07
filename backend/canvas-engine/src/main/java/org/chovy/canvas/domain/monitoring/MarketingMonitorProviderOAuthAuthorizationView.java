package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record MarketingMonitorProviderOAuthAuthorizationView(
        Long id,
        Long tenantId,
        String authState,
        String credentialKey,
        String providerType,
        String authType,
        String displayName,
        String status,
        String authorizationUrl,
        String authorizeEndpoint,
        String tokenEndpoint,
        String redirectUri,
        List<String> scopes,
        String codeChallengeMethod,
        Long credentialId,
        String providerError,
        String providerErrorDescription,
        Integer lastHttpStatus,
        String lastErrorMessage,
        LocalDateTime expiresAt,
        LocalDateTime completedAt,
        Map<String, Object> metadata,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
