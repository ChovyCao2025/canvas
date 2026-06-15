package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.canvas.api.ApiDefinitionFacade;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.ApiDefinitionCommand;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.ApiDefinitionListQuery;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.ApiDefinitionView;
import org.chovy.canvas.canvas.api.ApiDefinitionFacade.PageView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class ApiDefinitionControllerCompatibilityTest {

    @Test
    void exposesLegacyListCreateUpdateAndDeleteRoutesWithSuccessEnvelope() {
        RecordingApiDefinitionFacade facade = new RecordingApiDefinitionFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/canvas/api-definitions?page=2&size=3&enabled=1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.total").isEqualTo(1)
                .jsonPath("$.data.records[0].id").isEqualTo(11);

        assertThat(facade.lastQuery)
                .returns(2, ApiDefinitionListQuery::page)
                .returns(3, ApiDefinitionListQuery::size)
                .returns(1, ApiDefinitionListQuery::enabled);

        client.post()
                .uri("/canvas/api-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "apiKey": "orders-api",
                          "url": "https://example.com/orders"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.enabled").isEqualTo(1)
                .jsonPath("$.data.receiptStatuses").isEqualTo("[]");

        assertThat(facade.lastCommand.rateLimitPerSecPresent()).isFalse();

        client.put()
                .uri("/canvas/api-definitions/11")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "url": "https://example.com/orders-v2",
                          "rateLimitPerSec": null
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.id").isEqualTo(11)
                .jsonPath("$.data.rateLimitPerSec").doesNotExist();

        assertThat(facade.lastId).isEqualTo(11L);
        assertThat(facade.lastCommand.rateLimitPerSecPresent()).isTrue();
        assertThat(facade.lastCommand.rateLimitPerSec()).isNull();

        client.delete()
                .uri("/canvas/api-definitions/11")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.operations).containsExactly("list", "create", "update", "delete");
    }

    @Test
    void mapsIllegalArgumentExceptionToApi001BadRequestEnvelope() {
        RecordingApiDefinitionFacade facade = new RecordingApiDefinitionFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/api-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "apiKey": "bad-api",
                          "url": ""
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("url is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(ApiDefinitionFacade facade) {
        return WebTestClient.bindToController(new ApiDefinitionController(facade)).build();
    }

    private static final class RecordingApiDefinitionFacade implements ApiDefinitionFacade {
        private final List<String> operations = new ArrayList<>();
        private ApiDefinitionListQuery lastQuery;
        private ApiDefinitionCommand lastCommand;
        private Long lastId;

        @Override
        public PageView<ApiDefinitionView> list(ApiDefinitionListQuery query) {
            operations.add("list");
            lastQuery = query;
            return new PageView<>(1L, List.of(view(11L, "orders-api", 1, 3)));
        }

        @Override
        public ApiDefinitionView create(ApiDefinitionCommand command) {
            operations.add("create");
            lastCommand = command;
            if (command.url() == null || command.url().isBlank()) {
                throw new IllegalArgumentException("url is required");
            }
            return view(11L, command.apiKey(), 1, command.rateLimitPerSec());
        }

        @Override
        public ApiDefinitionView update(Long id, ApiDefinitionCommand command) {
            operations.add("update");
            lastId = id;
            lastCommand = command;
            return view(id, "orders-api", 1, command.rateLimitPerSec());
        }

        @Override
        public void delete(Long id) {
            operations.add("delete");
            lastId = id;
        }

        private static ApiDefinitionView view(Long id, String apiKey, Integer enabled, Integer rateLimitPerSec) {
            return new ApiDefinitionView(
                    id,
                    apiKey,
                    "https://example.com/orders",
                    enabled,
                    0,
                    0,
                    1440,
                    "[]",
                    rateLimitPerSec);
        }
    }
}
