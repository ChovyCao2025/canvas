package org.chovy.canvas.execution.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasNodeDefinition;
import org.chovy.canvas.execution.domain.DagRuntimeService;
import org.junit.jupiter.api.Test;

class CanvasTriggerRouteStoreTest {

    @Test
    void triggerServiceWritesRoutesThroughStorePort() {
        InMemoryTriggerRouteStore store = new InMemoryTriggerRouteStore();
        CanvasTriggerApplicationService service = new CanvasTriggerApplicationService(store);
        PublishedCanvasDefinition definition = new PublishedCanvasDefinition(
                1L,
                2L,
                3L,
                1,
                "{\"nodes\":[]}",
                Instant.parse("2026-06-10T05:00:00Z"),
                Map.of("triggerType", "MQ", "topicKey", "orders.created"),
                List.of(new PublishedCanvasNodeDefinition("start", "START", "Start", "{}", Map.of(), Map.of())),
                List.of());

        service.register(definition, new DagRuntimeService().validate(definition));

        assertThat(store.routes()).containsExactly(new CanvasTriggerApplicationService.TriggerRoute(
                1L,
                2L,
                3L,
                "MQ",
                "orders.created"));
    }
}
