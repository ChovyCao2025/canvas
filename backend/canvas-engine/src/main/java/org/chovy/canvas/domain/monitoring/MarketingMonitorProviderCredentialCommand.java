package org.chovy.canvas.domain.monitoring;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record MarketingMonitorProviderCredentialCommand(
        String credentialKey,
        String providerType,
        String authType,
        String displayName,
        Boolean enabled,
        String accessToken,
        String refreshToken,
        String apiKey,
        String tokenType,
        List<String> scopes,
        LocalDateTime expiresAt,
        LocalDateTime refreshTokenExpiresAt,
        String refreshEndpoint,
        String revokeEndpoint,
        String clientId,
        String clientSecret,
        Map<String, Object> metadata) {
}
