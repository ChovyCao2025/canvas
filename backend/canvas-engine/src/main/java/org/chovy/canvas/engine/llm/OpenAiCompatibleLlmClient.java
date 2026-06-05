package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final int DEFAULT_TIMEOUT_MS = 3_000;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String providerType) {
        String normalized = providerType == null ? "" : providerType.trim().toUpperCase(Locale.ROOT);
        return LlmProviderType.OPENAI_COMPATIBLE.equals(normalized) || "OPENAI".equals(normalized);
    }

    @Override
    public Mono<LlmResponse> complete(LlmRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.modelKey());
        body.put("messages", java.util.List.of(
                Map.of("role", "system", "content", "Return only a JSON object matching the requested schema."),
                Map.of("role", "user", "content", request.prompt())
        ));
        body.put("response_format", Map.of("type", "json_object"));
        if (request.params() != null) {
            request.params().forEach((key, value) -> {
                if (value != null && !body.containsKey(key)) {
                    body.put(key, value);
                }
            });
        }

        return webClientBuilder.build()
                .post()
                .uri(chatCompletionsEndpoint(request.endpoint()))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs(request.timeoutMs())))
                .map(this::parseResponse);
    }

    private LlmResponse parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            JsonNode output = parseJsonContent(content);
            JsonNode usage = root.path("usage");
            return new LlmResponse(
                    content,
                    output,
                    usage.path("prompt_tokens").isNumber() ? usage.path("prompt_tokens").asInt() : null,
                    usage.path("completion_tokens").isNumber() ? usage.path("completion_tokens").asInt() : null);
        } catch (Exception e) {
            throw new IllegalArgumentException("AI_LLM: provider response is not valid JSON", e);
        }
    }

    private JsonNode parseJsonContent(String content) {
        try {
            JsonNode parsed = objectMapper.readTree(content == null || content.isBlank() ? "{}" : content);
            if (!parsed.isObject()) {
                throw new IllegalArgumentException("AI_LLM: provider content must be a JSON object");
            }
            return parsed;
        } catch (Exception e) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("rawContent", content == null ? "" : content);
            return node;
        }
    }

    private static String chatCompletionsEndpoint(String endpoint) {
        String value = endpoint == null ? "" : endpoint.trim();
        if (value.endsWith("/chat/completions")) {
            return value;
        }
        if (value.endsWith("/")) {
            return value + "chat/completions";
        }
        return value + "/chat/completions";
    }

    private static int timeoutMs(int configured) {
        return configured <= 0 ? DEFAULT_TIMEOUT_MS : Math.min(configured, 30_000);
    }
}
