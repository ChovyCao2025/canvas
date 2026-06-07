package org.chovy.canvas.domain.search;

public interface SearchMarketingProviderReadClient {

    boolean supports(String provider, String runType);

    SearchMarketingProviderSyncResult sync(SearchMarketingSyncCommand command,
                                           SearchMarketingCredentialRef credential);
}
