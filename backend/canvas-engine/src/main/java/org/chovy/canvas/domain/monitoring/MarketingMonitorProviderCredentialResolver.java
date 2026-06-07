package org.chovy.canvas.domain.monitoring;

@FunctionalInterface
public interface MarketingMonitorProviderCredentialResolver {

    String resolve(String reference);

    default String resolve(Long tenantId, String reference) {
        return resolve(reference);
    }
}
