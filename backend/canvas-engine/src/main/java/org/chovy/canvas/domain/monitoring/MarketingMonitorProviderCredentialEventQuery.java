package org.chovy.canvas.domain.monitoring;

public record MarketingMonitorProviderCredentialEventQuery(
        String credentialKey,
        String eventType,
        String status,
        int limit) {
}
