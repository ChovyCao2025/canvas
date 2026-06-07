package org.chovy.canvas.domain.monitoring;

public record MarketingMonitorProviderOAuthAuthorizationQuery(
        String credentialKey,
        String providerType,
        String status,
        int limit) {
}
