package org.chovy.canvas.domain.search;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SearchMarketingProviderReadGateway {

    private final List<SearchMarketingProviderReadClient> clients;

    @Autowired
    public SearchMarketingProviderReadGateway(ObjectProvider<SearchMarketingProviderReadClient> clients) {
        this(clients == null ? List.of() : clients.orderedStream().toList());
    }

    SearchMarketingProviderReadGateway(List<SearchMarketingProviderReadClient> clients) {
        this.clients = clients == null ? List.of() : List.copyOf(clients);
    }

    public static SearchMarketingProviderReadGateway unsupported() {
        return new SearchMarketingProviderReadGateway(List.of());
    }

    public SearchMarketingProviderSyncResult sync(SearchMarketingSyncCommand command,
                                                  SearchMarketingCredentialRef credential) {
        if (command == null) {
            return SearchMarketingProviderSyncResult.failure(
                    "INVALID_SEARCH_SYNC_REQUEST",
                    "search marketing sync command is required",
                    false,
                    Map.of());
        }
        if (credential == null || !credential.available()) {
            return SearchMarketingProviderSyncResult.failure(
                    "SEARCH_PROVIDER_CREDENTIAL_UNAVAILABLE",
                    credential == null ? "search provider credential is unavailable" : credential.errorMessage(),
                    false,
                    Map.of("provider", command.provider(), "runType", command.runType()));
        }
        SearchMarketingProviderReadClient selectedClient = clients.stream()
                .filter(client -> client.supports(command.provider(), command.runType()))
                .findFirst()
                .orElse(null);
        if (selectedClient == null) {
            return SearchMarketingProviderSyncResult.failure(
                    "SEARCH_READ_CLIENT_UNAVAILABLE",
                    "No search marketing provider read client is registered for " + command.provider(),
                    false,
                    Map.of("provider", command.provider(), "runType", command.runType()));
        }
        return selectedClient.sync(command, credential);
    }
}
