package org.chovy.canvas.domain.search;

public interface GoogleAdsSearchMarketingTransport {

    SearchMarketingProviderMutationResult mutate(SearchMarketingProviderMutationRequest request,
                                                 SearchMarketingCredentialRef credential);
}
