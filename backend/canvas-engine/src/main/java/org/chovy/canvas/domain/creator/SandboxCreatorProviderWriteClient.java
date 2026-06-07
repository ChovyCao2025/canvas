package org.chovy.canvas.domain.creator;

import org.chovy.canvas.domain.providerwrite.ProviderWriteSandboxSupport;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SandboxCreatorProviderWriteClient implements CreatorProviderWriteClient {

    @Override
    public boolean supports(CreatorProviderMutationRequest request) {
        return request != null && ProviderWriteSandboxSupport.supportsSandboxProvider(request.provider());
    }

    @Override
    public CreatorProviderMutationResult execute(CreatorProviderMutationRequest request) {
        if (request == null) {
            return CreatorProviderMutationResult.failure(
                    "INVALID_REQUEST",
                    "creator provider mutation request is required",
                    Map.of());
        }
        if (!supports(request)) {
            return CreatorProviderMutationResult.failure(
                    "UNSUPPORTED_PROVIDER",
                    "Sandbox creator provider client only supports sandbox providers",
                    Map.of("provider", request.provider()));
        }
        return CreatorProviderMutationResult.success(
                ProviderWriteSandboxSupport.operationId("creator", request.provider(), request.mutationType(),
                        request.idempotencyKey(), request.dryRun()),
                ProviderWriteSandboxSupport.response(
                        "creator",
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
