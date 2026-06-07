package org.chovy.canvas.domain.monitoring;

public record MarketingMonitorProviderOAuthAuthorizationEventQuery(
        String authState,
        String credentialKey,
        String eventType,
        String status,
        int limit) {
}
