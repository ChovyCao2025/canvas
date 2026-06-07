package org.chovy.canvas.domain.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SearchMarketingProviderWriteGateway {

    private final List<SearchMarketingProviderWriteClient> clients;

    @Autowired
    public SearchMarketingProviderWriteGateway(ObjectProvider<SearchMarketingProviderWriteClient> clients) {
        this(clients == null ? List.of() : clients.orderedStream().toList());
    }

    SearchMarketingProviderWriteGateway(List<SearchMarketingProviderWriteClient> clients) {
        this.clients = clients == null ? List.of() : List.copyOf(clients);
    }

    public static SearchMarketingProviderWriteGateway unsupported() {
        return new SearchMarketingProviderWriteGateway(List.of());
    }

    public boolean supportsLiveApply(String provider) {
        if (provider == null || provider.isBlank()) {
            return false;
        }
        SearchMarketingProviderMutationRequest probe = new SearchMarketingProviderMutationRequest(
                0L,
                0L,
                provider,
                "readiness-probe",
                null,
                "UPDATE_KEYWORD_BID",
                "KEYWORD",
                "readiness-probe",
                "readiness-probe",
                false,
                true,
                Map.of(),
                Map.of());
        return clients.stream().anyMatch(client -> client.supports(probe));
    }

    public SearchMarketingProviderMutationResult execute(SearchMarketingProviderMutationRequest request) {
        if (request == null) {
            return SearchMarketingProviderMutationResult.failure(
                    "INVALID_REQUEST",
                    "provider mutation request is required",
                    Map.of());
        }
        SearchMarketingProviderWriteClient selectedClient = clients.stream()
                .filter(client -> client.supports(request))
                .findFirst()
                .orElse(null);
        if (selectedClient != null) {
            return selectedClient.execute(request);
        }
        if (request.dryRun()) {
            return SearchMarketingProviderMutationResult.success("dry-run",
                    Map.of(
                            "validated", true,
                            "provider", request.provider(),
                            "mutationType", request.mutationType(),
                            "partialFailure", request.partialFailure()));
        }
        return SearchMarketingProviderMutationResult.failure(
                "PROVIDER_CLIENT_UNAVAILABLE",
                "No live search marketing provider write client is registered for " + request.provider(),
                Map.of("provider", request.provider(), "mutationType", request.mutationType()));
    }
}
