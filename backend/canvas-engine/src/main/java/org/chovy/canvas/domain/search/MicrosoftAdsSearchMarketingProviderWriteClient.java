package org.chovy.canvas.domain.search;

import org.chovy.canvas.domain.providerwrite.ProviderWriteEvidenceSanitizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MicrosoftAdsSearchMarketingProviderWriteClient implements SearchMarketingProviderWriteClient {

    private final SearchMarketingCredentialResolver credentialResolver;
    private final MicrosoftAdsSearchMarketingTransport transport;

    @Autowired
    public MicrosoftAdsSearchMarketingProviderWriteClient(SearchMarketingCredentialResolver credentialResolver,
                                                         ObjectProvider<MicrosoftAdsSearchMarketingTransport> transports) {
        this(credentialResolver, transports == null ? null : transports.getIfAvailable());
    }

    MicrosoftAdsSearchMarketingProviderWriteClient(SearchMarketingCredentialResolver credentialResolver,
                                                   MicrosoftAdsSearchMarketingTransport transport) {
        this.credentialResolver = credentialResolver;
        this.transport = transport == null ? unavailableTransport() : transport;
    }

    @Override
    public boolean supports(SearchMarketingProviderMutationRequest request) {
        return request != null && "MICROSOFT_ADS".equalsIgnoreCase(request.provider());
    }

    @Override
    public SearchMarketingProviderMutationResult execute(SearchMarketingProviderMutationRequest request) {
        if (request == null) {
            return SearchMarketingProviderMutationResult.failure(
                    "INVALID_REQUEST", "search marketing mutation request is required", Map.of());
        }
        SearchMarketingCredentialRef credential = credentialResolver.resolve(
                request.tenantId(),
                request.provider(),
                credentialKey(request.metadata()));
        if (credential == null || !credential.available()) {
            return SearchMarketingProviderMutationResult.failure(
                    "SEARCH_PROVIDER_CREDENTIAL_UNAVAILABLE",
                    credential == null ? "search provider credential is unavailable" : credential.errorMessage(),
                    credential == null ? Map.of("provider", request.provider()) : credential.safeEvidence());
        }
        if (request.dryRun() && !supportsProviderValidation(request)) {
            return SearchMarketingProviderMutationResult.success(
                    "microsoft-ads-local-validation-" + request.idempotencyKey(),
                    Map.of(
                            "validated", true,
                            "validationMode", "LOCAL",
                            "provider", request.provider(),
                            "mutationType", request.mutationType()));
        }
        return sanitize(transport.mutate(request, credential));
    }

    private boolean supportsProviderValidation(SearchMarketingProviderMutationRequest request) {
        return "UPDATE_CAMPAIGN_BUDGET".equals(request.mutationType());
    }

    private String credentialKey(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get("credentialKey");
        return value == null ? null : String.valueOf(value);
    }

    private SearchMarketingProviderMutationResult sanitize(SearchMarketingProviderMutationResult result) {
        if (result == null) {
            return SearchMarketingProviderMutationResult.failure(
                    "SEARCH_PROVIDER_EMPTY_RESPONSE",
                    "search provider returned no mutation response",
                    Map.of());
        }
        Map<String, Object> response = ProviderWriteEvidenceSanitizer.sanitizeMap(result.response());
        return result.success()
                ? SearchMarketingProviderMutationResult.success(result.providerOperationId(), response)
                : SearchMarketingProviderMutationResult.failure(result.errorCode(), result.errorMessage(), response);
    }

    private MicrosoftAdsSearchMarketingTransport unavailableTransport() {
        return (request, credential) -> SearchMarketingProviderMutationResult.failure(
                "MICROSOFT_ADS_TRANSPORT_UNAVAILABLE",
                "Microsoft Ads search marketing transport is not configured",
                Map.of("provider", request.provider(), "mutationType", request.mutationType()));
    }
}
