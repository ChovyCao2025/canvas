package org.chovy.canvas.domain.search;

public interface SearchMarketingProviderWriteClient {

    boolean supports(SearchMarketingProviderMutationRequest request);

    SearchMarketingProviderMutationResult execute(SearchMarketingProviderMutationRequest request);
}
