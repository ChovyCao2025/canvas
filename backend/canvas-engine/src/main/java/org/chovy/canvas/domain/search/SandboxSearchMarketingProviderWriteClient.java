package org.chovy.canvas.domain.search;

import org.chovy.canvas.domain.providerwrite.ProviderWriteSandboxSupport;
import org.springframework.stereotype.Component;

@Component
public class SandboxSearchMarketingProviderWriteClient implements SearchMarketingProviderWriteClient {

    @Override
    public boolean supports(SearchMarketingProviderMutationRequest request) {
        return request != null && ProviderWriteSandboxSupport.supportsSandboxProvider(request.provider());
    }

    @Override
    public SearchMarketingProviderMutationResult execute(SearchMarketingProviderMutationRequest request) {
        if (request == null) {
            return SearchMarketingProviderMutationResult.failure(
                    "INVALID_REQUEST",
                    "search marketing provider mutation request is required",
                    java.util.Map.of());
        }
        if (!supports(request)) {
            return SearchMarketingProviderMutationResult.failure(
                    "UNSUPPORTED_PROVIDER",
                    "Sandbox search marketing client only supports sandbox providers",
                    java.util.Map.of("provider", request.provider()));
        }
        return SearchMarketingProviderMutationResult.success(
                ProviderWriteSandboxSupport.operationId("search", request.provider(), request.mutationType(),
                        request.idempotencyKey(), request.dryRun()),
                ProviderWriteSandboxSupport.response(
                        "search",
                        request.provider(),
                        request.mutationType(),
                        request.entityType(),
                        request.externalEntityId(),
                        request.idempotencyKey(),
                        request.dryRun(),
                        request.partialFailure(),
                        request.payload(),
                        request.metadata()));
    }
}
