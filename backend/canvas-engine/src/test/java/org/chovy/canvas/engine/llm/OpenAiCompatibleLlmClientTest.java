package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleLlmClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void postsChatCompletionAndParsesJsonContentWithUsage() {
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(
                WebClient.builder().exchangeFunction(request -> {
                    requestedUrl.set(request.url().toString());
                    return reactor.core.publisher.Mono.just(ClientResponse.create(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .body("""
                                    {
                                      "choices": [
                                        {"message": {"content": "{\\"text\\":\\"hello\\",\\"tone\\":\\"warm\\"}"}}
                                      ],
                                      "usage": {"prompt_tokens": 12, "completion_tokens": 4}
                                    }
                                    """)
                            .build());
                }),
                objectMapper);

        LlmClient.LlmResponse response = client.complete(new LlmClient.LlmRequest(
                "https://ai.example.test/v1",
                "gpt-test",
                "Generate JSON",
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode(),
                Map.of("temperature", 0.1),
                1000)).block();

        assertThat(requestedUrl.get()).isEqualTo("https://ai.example.test/v1/chat/completions");
        assertThat(response.output().path("text").asText()).isEqualTo("hello");
        assertThat(response.promptTokens()).isEqualTo(12);
        assertThat(response.completionTokens()).isEqualTo(4);
    }

    @Test
    void supportsOpenAiCompatibleProviderTypes() {
        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(WebClient.builder(), objectMapper);

        assertThat(client.supports("OPENAI_COMPATIBLE")).isTrue();
        assertThat(client.supports("openai")).isTrue();
        assertThat(client.supports("MOCK")).isFalse();
    }
}
