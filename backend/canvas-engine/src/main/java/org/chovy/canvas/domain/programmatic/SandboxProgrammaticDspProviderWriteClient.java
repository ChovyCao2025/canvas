package org.chovy.canvas.domain.programmatic;

import org.chovy.canvas.domain.providerwrite.ProviderWriteSandboxSupport;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SandboxProgrammaticDspProviderWriteClient implements ProgrammaticDspProviderWriteClient {

    @Override
    public boolean supports(ProgrammaticDspMutationRequest request) {
        return request != null && ProviderWriteSandboxSupport.supportsSandboxProvider(request.provider());
    }

    @Override
    public ProgrammaticDspMutationResult execute(ProgrammaticDspMutationRequest request) {
        if (request == null) {
            return ProgrammaticDspMutationResult.failure(
                    "INVALID_REQUEST",
                    "programmatic DSP mutation request is required",
                    Map.of());
        }
        if (!supports(request)) {
            return ProgrammaticDspMutationResult.failure(
                    "UNSUPPORTED_PROVIDER",
                    "Sandbox programmatic DSP provider client only supports sandbox providers",
                    Map.of("provider", request.provider()));
        }
        return ProgrammaticDspMutationResult.success(
                ProviderWriteSandboxSupport.operationId("programmatic", request.provider(), request.mutationType(),
                        request.idempotencyKey(), request.dryRun()),
                ProviderWriteSandboxSupport.response(
                        "programmatic",
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
