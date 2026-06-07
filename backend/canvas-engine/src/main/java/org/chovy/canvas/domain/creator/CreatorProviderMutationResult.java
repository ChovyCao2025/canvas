package org.chovy.canvas.domain.creator;

import java.util.Map;

public record CreatorProviderMutationResult(
        boolean success,
        String providerOperationId,
        String errorCode,
        String errorMessage,
        Map<String, Object> response
) {

    public static CreatorProviderMutationResult success(String providerOperationId, Map<String, Object> response) {
        return new CreatorProviderMutationResult(true, providerOperationId, null, null,
                response == null ? Map.of() : response);
    }

    public static CreatorProviderMutationResult failure(String errorCode,
                                                        String errorMessage,
                                                        Map<String, Object> response) {
        return new CreatorProviderMutationResult(false, null, errorCode, errorMessage,
                response == null ? Map.of() : response);
    }
}
