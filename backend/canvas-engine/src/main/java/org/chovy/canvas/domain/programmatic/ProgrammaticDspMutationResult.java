package org.chovy.canvas.domain.programmatic;

import java.util.Map;

public record ProgrammaticDspMutationResult(
        boolean success,
        String providerOperationId,
        String errorCode,
        String errorMessage,
        Map<String, Object> response
) {

    public static ProgrammaticDspMutationResult success(String providerOperationId, Map<String, Object> response) {
        return new ProgrammaticDspMutationResult(true, providerOperationId, null, null,
                response == null ? Map.of() : response);
    }

    public static ProgrammaticDspMutationResult failure(String errorCode,
                                                       String errorMessage,
                                                       Map<String, Object> response) {
        return new ProgrammaticDspMutationResult(false, null, errorCode, errorMessage,
                response == null ? Map.of() : response);
    }
}
