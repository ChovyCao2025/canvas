package org.chovy.canvas.domain.creator;

public interface CreatorProviderWriteClient {

    boolean supports(CreatorProviderMutationRequest request);

    CreatorProviderMutationResult execute(CreatorProviderMutationRequest request);
}
