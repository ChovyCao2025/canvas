package org.chovy.canvas.domain.search;

import java.util.Map;

public record SearchMarketingProviderMutationResult(
        boolean success,
        String providerOperationId,
        String errorCode,
        String errorMessage,
        Map<String, Object> response
) {

    public static SearchMarketingProviderMutationResult success(String providerOperationId,
                                                                Map<String, Object> response) {
        return new SearchMarketingProviderMutationResult(true, providerOperationId, null, null,
                response == null ? Map.of() : response);
    }

    public static SearchMarketingProviderMutationResult failure(String errorCode,
                                                                String errorMessage,
                                                                Map<String, Object> response) {
        return new SearchMarketingProviderMutationResult(false, null, errorCode, errorMessage,
                response == null ? Map.of() : response);
    }
}
