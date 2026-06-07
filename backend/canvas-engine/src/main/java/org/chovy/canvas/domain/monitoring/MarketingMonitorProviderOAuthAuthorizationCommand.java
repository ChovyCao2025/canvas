package org.chovy.canvas.domain.monitoring;

import java.util.List;
import java.util.Map;

public record MarketingMonitorProviderOAuthAuthorizationCommand(
        String credentialKey,
        String providerType,
        String authType,
        String displayName,
        String authorizeEndpoint,
        String tokenEndpoint,
        String revokeEndpoint,
        String redirectUri,
        String clientId,
        String clientSecret,
        List<String> scopes,
        Map<String, Object> authorizeParams,
        Integer expiresInMinutes,
        Map<String, Object> metadata) {
}
