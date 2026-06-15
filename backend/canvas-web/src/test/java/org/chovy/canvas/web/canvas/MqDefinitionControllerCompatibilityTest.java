package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.canvas.api.MqDefinitionFacade;
import org.chovy.canvas.canvas.api.MqDefinitionFacade.MqDefinitionCommand;
import org.chovy.canvas.canvas.api.MqDefinitionFacade.MqDefinitionListQuery;
import org.chovy.canvas.canvas.api.MqDefinitionFacade.MqDefinitionView;
import org.chovy.canvas.canvas.api.MqDefinitionFacade.PageView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class MqDefinitionControllerCompatibilityTest {

    @Test
    void exposesLegacyListCreateUpdateAndDeleteRoutesWithSuccessEnvelope() {
        RecordingMqDefinitionFacade facade = new RecordingMqDefinitionFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/canvas/mq-definitions?page=2&size=3&enabled=1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.message").isEqualTo("success")
                .jsonPath("$.errorCode").doesNotExist()
                .jsonPath("$.traceId").doesNotExist()
                .jsonPath("$.data.total").isEqualTo(1)
                .jsonPath("$.data.list[0].id").isEqualTo(11)
                .jsonPath("$.data.list[0].messageCode").isEqualTo("ORDER_PAID")
                .jsonPath("$.data.records").doesNotExist();

        assertThat(facade.lastQuery)
                .returns(2, MqDefinitionListQuery::page)
                .returns(3, MqDefinitionListQuery::size)
                .returns(1, MqDefinitionListQuery::enabled);

        client.post()
                .uri("/canvas/mq-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "messageCode": "ORDER_PAID",
                          "topic": "order-topic",
                          "enabled": 1
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.id").isEqualTo(11)
                .jsonPath("$.data.topic").isEqualTo("order-topic");

        assertThat(facade.lastCommand)
                .returns("ORDER_PAID", MqDefinitionCommand::messageCode)
                .returns("order-topic", MqDefinitionCommand::topic)
                .returns(1, MqDefinitionCommand::enabled);

        client.put()
                .uri("/canvas/mq-definitions/11")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "id": 99,
                          "messageCode": "ORDER_COMPLETED",
                          "topic": "order-topic-v2"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.id").isEqualTo(11)
                .jsonPath("$.data.messageCode").isEqualTo("ORDER_COMPLETED");

        assertThat(facade.lastId).isEqualTo(11L);

        client.delete()
                .uri("/canvas/mq-definitions/11")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.operations).containsExactly("list", "create", "update", "delete");
    }

    @Test
    void mapsIllegalArgumentExceptionToApi001BadRequestEnvelope() {
        RecordingMqDefinitionFacade facade = new RecordingMqDefinitionFacade();
        facade.failure = new IllegalArgumentException("messageCode is required");

        webClient(facade)
                .post()
                .uri("/canvas/mq-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"messageCode": "", "topic": "order-topic"}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("messageCode is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(MqDefinitionFacade facade) {
        return WebTestClient.bindToController(new MqDefinitionController(facade)).build();
    }

    private static final class RecordingMqDefinitionFacade implements MqDefinitionFacade {
        private final List<String> operations = new ArrayList<>();
        private MqDefinitionListQuery lastQuery;
        private MqDefinitionCommand lastCommand;
        private Long lastId;
        private IllegalArgumentException failure;

        @Override
        public PageView<MqDefinitionView> list(MqDefinitionListQuery query) {
            failIfConfigured();
            operations.add("list");
            lastQuery = query;
            return new PageView<>(1L, List.of(view(11L, "ORDER_PAID", "order-topic", 1)));
        }

        @Override
        public MqDefinitionView create(MqDefinitionCommand command) {
            failIfConfigured();
            operations.add("create");
            lastCommand = command;
            return view(11L, command.messageCode(), command.topic(), command.enabled());
        }

        @Override
        public MqDefinitionView update(Long id, MqDefinitionCommand command) {
            failIfConfigured();
            operations.add("update");
            lastId = id;
            lastCommand = command;
            return view(id, command.messageCode(), command.topic(), 1);
        }

        @Override
        public void delete(Long id) {
            failIfConfigured();
            operations.add("delete");
            lastId = id;
        }

        private void failIfConfigured() {
            if (failure != null) {
                throw failure;
            }
        }

        private static MqDefinitionView view(Long id, String messageCode, String topic, Integer enabled) {
            return new MqDefinitionView(
                    id,
                    messageCode,
                    topic,
                    "tag-a",
                    "consumer-a",
                    "{}",
                    "description",
                    enabled,
                    "operator-1",
                    LocalDateTime.parse("2026-06-15T10:00:00"),
                    LocalDateTime.parse("2026-06-15T10:00:00"));
        }
    }
}
