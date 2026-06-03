package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceEvaluationContextFetcherTest {

    @Test
    void fetchAsyncDefersRemoteCallUntilSubscribedAndReturnsTags() {
        AtomicInteger remoteCalls = new AtomicInteger();
        AudienceEvaluationContextFetcher fetcher = new AudienceEvaluationContextFetcher(new ObjectMapper());

        Mono<Map<String, Object>> result = fetcher.fetchAsync(
                webClient(remoteCalls),
                "u001",
                """
                        {
                          "logic": "AND",
                          "conditions": [
                            {"field": "level", "op": "=", "value": "gold"}
                          ]
                        }
                        """);

        assertThat(remoteCalls).hasValue(0);

        StepVerifier.create(result)
                .assertNext(context -> assertThat(context).containsEntry("level", "gold"))
                .verifyComplete();
        assertThat(remoteCalls).hasValue(1);
    }

    @Test
    void fetchAsyncDoesNotCallRemoteServiceWhenRuleHasNoFields() {
        AtomicInteger remoteCalls = new AtomicInteger();
        AudienceEvaluationContextFetcher fetcher = new AudienceEvaluationContextFetcher(new ObjectMapper());

        StepVerifier.create(fetcher.fetchAsync(
                        webClient(remoteCalls),
                        "u001",
                        """
                                {
                                  "logic": "AND",
                                  "conditions": []
                                }
                                """))
                .expectNext(Map.of())
                .verifyComplete();

        assertThat(remoteCalls).hasValue(0);
    }

    private static WebClient webClient(AtomicInteger remoteCalls) {
        ExchangeFunction exchangeFunction = request -> {
            remoteCalls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                            {
                              "tags": {
                                "level": "gold"
                              }
                            }
                            """)
                    .build());
        };
        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }
}
