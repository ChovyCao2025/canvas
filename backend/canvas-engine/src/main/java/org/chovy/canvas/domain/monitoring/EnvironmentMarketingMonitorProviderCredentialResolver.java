package org.chovy.canvas.domain.monitoring;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentMarketingMonitorProviderCredentialResolver implements MarketingMonitorProviderCredentialResolver {

    private final ObjectProvider<MarketingMonitorProviderCredentialService> credentialServiceProvider;

    public EnvironmentMarketingMonitorProviderCredentialResolver(
            ObjectProvider<MarketingMonitorProviderCredentialService> credentialServiceProvider) {
        this.credentialServiceProvider = credentialServiceProvider;
    }

    @Override
    public String resolve(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        String key = reference.trim();
        String property = System.getProperty(key);
        if (property != null && !property.isBlank()) {
            return property;
        }
        String value = System.getenv(key);
        return value == null || value.isBlank() ? null : value;
    }

    @Override
    public String resolve(Long tenantId, String reference) {
        if (reference != null && reference.trim().startsWith("credential:")) {
            MarketingMonitorProviderCredentialService service =
                    credentialServiceProvider == null ? null : credentialServiceProvider.getIfAvailable();
            if (service == null) {
                return null;
            }
            return service.resolveValue(tenantId, reference);
        }
        return resolve(reference);
    }
}
