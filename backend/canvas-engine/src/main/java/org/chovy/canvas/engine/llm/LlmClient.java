package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface LlmClient {

    boolean supports(String providerType);

    Mono<LlmResponse> complete(LlmRequest request);

    record LlmRequest(
            String endpoint,
            String modelKey,
            String prompt,
            JsonNode outputSchema,
            JsonNode defaultValues,
            Map<String, Object> params,
            int timeoutMs) {
    }

    record LlmResponse(
            String rawContent,
            JsonNode output,
            Integer promptTokens,
            Integer completionTokens) {
    }
}
