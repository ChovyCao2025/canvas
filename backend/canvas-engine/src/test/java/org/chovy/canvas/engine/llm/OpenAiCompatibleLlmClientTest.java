package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.codec.Hints;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleLlmClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void postsChatCompletionAndParsesJsonContentWithUsage() throws Exception {
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(
                WebClient.builder().exchangeFunction(request -> {
                    requestedUrl.set(request.url().toString());
                    authorizationHeader.set(request.headers().getFirst("Authorization"));
                    requestBody.set(captureBody(request));
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
                Map.of("temperature", 0.1, "maxTokens", 777),
                1000,
                "sk-live-secret")).block();

        assertThat(requestedUrl.get()).isEqualTo("https://ai.example.test/v1/chat/completions");
        assertThat(authorizationHeader.get()).isEqualTo("Bearer sk-live-secret");
        assertThat(objectMapper.readTree(requestBody.get()).path("max_tokens").asInt()).isEqualTo(777);
        assertThat(objectMapper.readTree(requestBody.get()).has("maxTokens")).isFalse();
        assertThat(response.output().path("text").asText()).isEqualTo("hello");
        assertThat(response.promptTokens()).isEqualTo(12);
        assertThat(response.completionTokens()).isEqualTo(4);
    }

    @Test
    void invalidJsonContentIsRejectedInsteadOfWrappedAsRawContent() {
        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(
                WebClient.builder().exchangeFunction(request -> reactor.core.publisher.Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body("""
                                {
                                  "choices": [
                                    {"message": {"content": "not json"}}
                                  ],
                                  "usage": {"prompt_tokens": 12, "completion_tokens": 4}
                                }
                                """)
                        .build())),
                objectMapper);

        StepVerifier.create(client.complete(new LlmClient.LlmRequest(
                        "https://ai.example.test/v1",
                        "gpt-test",
                        "Generate JSON",
                        objectMapper.createObjectNode(),
                        objectMapper.createObjectNode(),
                        Map.of(),
                        1000,
                        "sk-live-secret")))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(LlmInvalidJsonException.class)
                        .hasMessageContaining("provider content"))
                .verify();
    }

    @Test
    void supportsOpenAiCompatibleProviderTypes() {
        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(WebClient.builder(), objectMapper);

        assertThat(client.supports("OPENAI_COMPATIBLE")).isTrue();
        assertThat(client.supports("openai")).isTrue();
        assertThat(client.supports("MOCK")).isFalse();
    }

    private String captureBody(ClientRequest request) {
        MockClientHttpRequest mockRequest = new MockClientHttpRequest(request.method(), request.url());
        request.body().insert(mockRequest, new BodyInserter.Context() {
            @Override
            public List<HttpMessageWriter<?>> messageWriters() {
                return HandlerStrategies.withDefaults().messageWriters();
            }

            @Override
            public Optional<ServerHttpRequest> serverRequest() {
                return Optional.empty();
            }

            @Override
            public Map<String, Object> hints() {
                return Hints.none();
            }
        }).block();
        return mockRequest.getBodyAsString().block();
    }
}
