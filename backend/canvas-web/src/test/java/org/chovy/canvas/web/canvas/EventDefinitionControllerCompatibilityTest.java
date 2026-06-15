package org.chovy.canvas.web.canvas;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.canvas.api.EventDefinitionFacade;
import org.chovy.canvas.canvas.api.EventDefinitionFacade.EventDefinitionCommand;
import org.chovy.canvas.canvas.api.EventDefinitionFacade.EventDefinitionListQuery;
import org.chovy.canvas.canvas.api.EventDefinitionFacade.EventDefinitionView;
import org.chovy.canvas.canvas.api.EventDefinitionFacade.PageView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class EventDefinitionControllerCompatibilityTest {

    @Test
    void exposesLegacyListCreateUpdateAndDeleteRoutesWithSuccessEnvelope() {
        RecordingEventDefinitionFacade facade = new RecordingEventDefinitionFacade();
        WebTestClient client = webClient(facade);

        client.get()
                .uri("/canvas/event-definitions?page=2&size=3&enabled=1")
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
                .jsonPath("$.data.list[0].eventCode").isEqualTo("ORDER_PAID")
                .jsonPath("$.data.records").doesNotExist();

        assertThat(facade.lastQuery)
                .returns(2, EventDefinitionListQuery::page)
                .returns(3, EventDefinitionListQuery::size)
                .returns(1, EventDefinitionListQuery::enabled);

        client.post()
                .uri("/canvas/event-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "Order paid",
                          "eventCode": "ORDER_PAID",
                          "enabled": 1
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.id").isEqualTo(11)
                .jsonPath("$.data.eventCode").isEqualTo("ORDER_PAID");

        assertThat(facade.lastCommand)
                .returns("Order paid", EventDefinitionCommand::name)
                .returns("ORDER_PAID", EventDefinitionCommand::eventCode)
                .returns(1, EventDefinitionCommand::enabled);

        client.put()
                .uri("/canvas/event-definitions/11")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "eventCode": "ORDER_COMPLETED",
                          "description": "Order completed event"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.id").isEqualTo(11)
                .jsonPath("$.data.eventCode").isEqualTo("ORDER_COMPLETED");

        assertThat(facade.lastId).isEqualTo(11L);
        assertThat(facade.lastCommand.description()).isEqualTo("Order completed event");

        client.delete()
                .uri("/canvas/event-definitions/11")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data").doesNotExist();

        assertThat(facade.operations).containsExactly("list", "create", "update", "delete");
    }

    @Test
    void mapsIllegalArgumentExceptionToApi001BadRequestEnvelope() {
        RecordingEventDefinitionFacade facade = new RecordingEventDefinitionFacade();
        WebTestClient client = webClient(facade);

        client.post()
                .uri("/canvas/event-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "name": "Missing code",
                          "eventCode": ""
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.errorCode").isEqualTo("API_001")
                .jsonPath("$.message").isEqualTo("eventCode is required")
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.traceId").doesNotExist();
    }

    private static WebTestClient webClient(EventDefinitionFacade facade) {
        return WebTestClient.bindToController(new EventDefinitionController(facade)).build();
    }

    private static final class RecordingEventDefinitionFacade implements EventDefinitionFacade {
        private final List<String> operations = new ArrayList<>();
        private EventDefinitionListQuery lastQuery;
        private EventDefinitionCommand lastCommand;
        private Long lastId;

        @Override
        public PageView<EventDefinitionView> list(EventDefinitionListQuery query) {
            operations.add("list");
            lastQuery = query;
            return new PageView<>(1L, List.of(view(11L, "Order paid", "ORDER_PAID", 1)));
        }

        @Override
        public EventDefinitionView create(EventDefinitionCommand command) {
            operations.add("create");
            lastCommand = command;
            if (command.eventCode() == null || command.eventCode().isBlank()) {
                throw new IllegalArgumentException("eventCode is required");
            }
            return view(11L, command.name(), command.eventCode(), command.enabled());
        }

        @Override
        public EventDefinitionView update(Long id, EventDefinitionCommand command) {
            operations.add("update");
            lastId = id;
            lastCommand = command;
            return view(id, "Order paid", command.eventCode(), 1);
        }

        @Override
        public void delete(Long id) {
            operations.add("delete");
            lastId = id;
        }

        private static EventDefinitionView view(Long id, String name, String eventCode, Integer enabled) {
            return new EventDefinitionView(
                    id,
                    name,
                    eventCode,
                    "[]",
                    "description",
                    0,
                    "REJECT_UNKNOWN",
                    enabled,
                    "operator-1",
                    LocalDateTime.parse("2026-06-15T10:00:00"),
                    LocalDateTime.parse("2026-06-15T10:00:00"));
        }
    }
}
