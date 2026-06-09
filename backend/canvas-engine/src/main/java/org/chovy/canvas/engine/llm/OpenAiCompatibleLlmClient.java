package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        return LlmProviderType.OPENAI_COMPATIBLE.equals(normalized)
                || LlmProviderType.CUSTOM_OPENAI_COMPATIBLE.equals(normalized)
                || "OPENAI".equals(normalized);
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
                String bodyKey = bodyParamKey(key);
                Object bodyValue = bodyParamValue(bodyKey, value);
                if (bodyKey != null && bodyValue != null && !body.containsKey(bodyKey)) {
                    body.put(bodyKey, bodyValue);
                }
            });
        }

        return webClientBuilder.build()
                .post()
                .uri(chatCompletionsEndpoint(request.endpoint()))
                .headers(headers -> {
                    if (request.apiKey() != null && !request.apiKey().isBlank()) {
                        headers.setBearerAuth(request.apiKey().trim());
                    }
                })
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
        } catch (LlmInvalidJsonException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmInvalidJsonException("AI_LLM: provider response is not valid JSON", e);
        }
    }

    private JsonNode parseJsonContent(String content) {
        try {
            JsonNode parsed = objectMapper.readTree(content == null || content.isBlank() ? "{}" : content);
            if (!parsed.isObject()) {
                throw new LlmInvalidJsonException("AI_LLM: provider content must be a JSON object");
            }
            return parsed;
        } catch (LlmInvalidJsonException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmInvalidJsonException("AI_LLM: provider content is not valid JSON", e);
        }
    }

    private static String bodyParamKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String trimmed = key.trim();
        return "maxTokens".equals(trimmed) ? "max_tokens" : trimmed;
    }

    private static Object bodyParamValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        if ("max_tokens".equals(key)) {
            Integer maxTokens = intParam(value);
            return maxTokens == null ? null : Math.max(1, Math.min(maxTokens, 8_000));
        }
        if ("temperature".equals(key)) {
            Double temperature = doubleParam(value);
            return temperature == null ? null : Math.max(0.0, Math.min(temperature, 2.0));
        }
        return value;
    }

    private static Integer intParam(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Double doubleParam(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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
