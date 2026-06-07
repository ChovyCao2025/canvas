package org.chovy.canvas.domain.programmatic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProgrammaticDspProviderWriteGateway {

    private final List<ProgrammaticDspProviderWriteClient> clients;

    @Autowired
    public ProgrammaticDspProviderWriteGateway(ObjectProvider<ProgrammaticDspProviderWriteClient> clients) {
        this(clients == null ? List.of() : clients.orderedStream().toList());
    }

    ProgrammaticDspProviderWriteGateway(List<ProgrammaticDspProviderWriteClient> clients) {
        this.clients = clients == null ? List.of() : List.copyOf(clients);
    }

    public static ProgrammaticDspProviderWriteGateway unsupported() {
        return new ProgrammaticDspProviderWriteGateway(List.of());
    }

    public ProgrammaticDspMutationResult execute(ProgrammaticDspMutationRequest request) {
        if (request == null) {
            return ProgrammaticDspMutationResult.failure(
                    "INVALID_REQUEST",
                    "programmatic DSP mutation request is required",
                    Map.of());
        }
        ProgrammaticDspProviderWriteClient selectedClient = clients.stream()
                .filter(client -> client.supports(request))
                .findFirst()
                .orElse(null);
        if (selectedClient != null) {
            return selectedClient.execute(request);
        }
        if (request.dryRun()) {
            return ProgrammaticDspMutationResult.success("dry-run",
                    Map.of(
                            "validated", true,
                            "provider", request.provider(),
                            "mutationType", request.mutationType(),
                            "partialFailure", request.partialFailure()));
        }
        return ProgrammaticDspMutationResult.failure(
                "PROVIDER_CLIENT_UNAVAILABLE",
                "No live programmatic DSP provider write client is registered for " + request.provider(),
                Map.of("provider", request.provider(), "mutationType", request.mutationType()));
    }
}
