package org.chovy.canvas.domain.monitoring;

public record MarketingMonitorProviderCredentialQuery(
        String providerType,
        String authType,
        String status,
        int limit) {
}
