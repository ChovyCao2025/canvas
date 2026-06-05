package org.chovy.canvas.infrastructure.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * WebClient-backed adapter for outbound JSON integrations.
 */
@Component
public class WebClientExternalHttpClient implements ExternalHttpClient {

    private final WebClient reachPlatformClient;

    public WebClientExternalHttpClient(
            WebClient.Builder webClientBuilder,
            @Value("${canvas.integration.reach-platform-url}") String reachPlatformUrl
    ) {
        this.reachPlatformClient = webClientBuilder.clone().baseUrl(reachPlatformUrl).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> postJson(String integrationName, String path, Map<String, Object> payload) {
        WebClient client = switch (integrationName) {
            case ExternalHttpClient.REACH_PLATFORM -> reachPlatformClient;
            default -> throw new IllegalArgumentException("Unknown external integration: " + integrationName);
        };
        return client.post()
                .uri(path)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .map(map -> (Map<String, Object>) map);
    }
}
