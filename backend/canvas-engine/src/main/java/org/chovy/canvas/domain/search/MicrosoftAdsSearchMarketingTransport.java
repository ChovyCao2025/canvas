package org.chovy.canvas.domain.search;

public interface MicrosoftAdsSearchMarketingTransport {

    SearchMarketingProviderMutationResult mutate(SearchMarketingProviderMutationRequest request,
                                                 SearchMarketingCredentialRef credential);
}
