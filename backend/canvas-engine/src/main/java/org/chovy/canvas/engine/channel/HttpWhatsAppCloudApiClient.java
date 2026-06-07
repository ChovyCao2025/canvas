package org.chovy.canvas.engine.channel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Component
public class HttpWhatsAppCloudApiClient implements WhatsAppCloudApiClient {

    private final WebClient webClient;
    private final String apiVersion;
    private final Duration timeout;

    public HttpWhatsAppCloudApiClient(WebClient.Builder webClientBuilder,
                                      @Value("${canvas.conversation.whatsapp.cloud.graph-base-url:https://graph.facebook.com}")
                                      String graphBaseUrl,
                                      @Value("${canvas.conversation.whatsapp.cloud.graph-api-version:}") String apiVersion,
                                      @Value("${canvas.conversation.whatsapp.cloud.timeout-ms:10000}") long timeoutMs) {
        this.webClient = (webClientBuilder == null ? WebClient.builder() : webClientBuilder)
                .baseUrl(blankToDefault(graphBaseUrl, "https://graph.facebook.com"))
                .build();
        this.apiVersion = apiVersion == null ? "" : apiVersion.trim();
        this.timeout = Duration.ofMillis(Math.max(timeoutMs, 1L));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> sendMessage(String phoneNumberId,
                                           String accessToken,
                                           Map<String, Object> payload) {
        if (apiVersion.isBlank()) {
            throw new IllegalStateException("WhatsApp Graph API version is not configured");
        }
        Object response = webClient.post()
                .uri("/{version}/{phoneNumberId}/messages", apiVersion, phoneNumberId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(payload == null ? Map.of() : payload)
                .retrieve()
                .bodyToMono(Map.class)
                .block(timeout);
        return response instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
