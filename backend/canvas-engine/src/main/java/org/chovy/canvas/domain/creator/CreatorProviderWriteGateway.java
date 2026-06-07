package org.chovy.canvas.domain.creator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CreatorProviderWriteGateway {

    private final List<CreatorProviderWriteClient> clients;

    @Autowired
    public CreatorProviderWriteGateway(ObjectProvider<CreatorProviderWriteClient> clients) {
        this(clients == null ? List.of() : clients.orderedStream().toList());
    }

    CreatorProviderWriteGateway(List<CreatorProviderWriteClient> clients) {
        this.clients = clients == null ? List.of() : List.copyOf(clients);
    }

    public static CreatorProviderWriteGateway unsupported() {
        return new CreatorProviderWriteGateway(List.of());
    }

    public CreatorProviderMutationResult execute(CreatorProviderMutationRequest request) {
        if (request == null) {
            return CreatorProviderMutationResult.failure(
                    "INVALID_REQUEST",
                    "creator provider mutation request is required",
                    Map.of());
        }
        CreatorProviderWriteClient selectedClient = clients.stream()
                .filter(client -> client.supports(request))
                .findFirst()
                .orElse(null);
        if (selectedClient != null) {
            return selectedClient.execute(request);
        }
        if (request.dryRun()) {
            return CreatorProviderMutationResult.success("dry-run",
                    Map.of(
                            "validated", true,
                            "provider", request.provider(),
                            "mutationType", request.mutationType(),
                            "partialFailure", request.partialFailure()));
        }
        return CreatorProviderMutationResult.failure(
                "PROVIDER_CLIENT_UNAVAILABLE",
                "No live creator provider write client is registered for " + request.provider(),
                Map.of("provider", request.provider(), "mutationType", request.mutationType()));
    }
}
