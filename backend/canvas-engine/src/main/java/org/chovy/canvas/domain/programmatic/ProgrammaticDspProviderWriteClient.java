package org.chovy.canvas.domain.programmatic;

public interface ProgrammaticDspProviderWriteClient {

    boolean supports(ProgrammaticDspMutationRequest request);

    ProgrammaticDspMutationResult execute(ProgrammaticDspMutationRequest request);
}
