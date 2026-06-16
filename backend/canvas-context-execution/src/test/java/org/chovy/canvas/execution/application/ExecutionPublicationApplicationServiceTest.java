package org.chovy.canvas.execution.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasEdgeDefinition;
import org.chovy.canvas.canvas.api.PublishedCanvasNodeDefinition;
import org.chovy.canvas.execution.domain.DagRuntimeService;
import org.junit.jupiter.api.Test;

class ExecutionPublicationApplicationServiceTest {

    @Test
    void publishValidatesDagBeforeRegisteringRuntimeState() {
        ExecutionDefinitionRepository repository = new InMemoryExecutionDefinitionRepository();
        CanvasTriggerApplicationService triggerService = new CanvasTriggerApplicationService();
        CanvasSchedulerApplicationService schedulerService = new CanvasSchedulerApplicationService();
        ExecutionPublicationApplicationService service = new ExecutionPublicationApplicationService(
                new DagRuntimeService(),
                triggerService,
                schedulerService,
                repository);

        PublishedCanvasDefinition definition = definition(
                List.of(
                        node("start", "START"),
                        node("message", "SEND_MESSAGE"),
                        node("end", "END")),
                List.of(
                        edge("start", "message"),
                        edge("message", "end")),
                Map.of(
                        "triggerType", "MQ",
                        "topicKey", "orders.created",
                        "scheduleCron", "0 0/5 * * * ?"));

        service.publish(definition);

        assertThat(repository.getPublished(7L, 11L)).isEqualTo(definition);
        assertThat(triggerService.routes())
                .containsExactly(new CanvasTriggerApplicationService.TriggerRoute(
                        7L,
                        11L,
                        13L,
                        "MQ",
                        "orders.created"));
        assertThat(schedulerService.registrations())
                .containsExactly(new CanvasSchedulerApplicationService.ScheduleRegistration(
                        7L,
                        11L,
                        13L,
                        "0 0/5 * * * ?"));
    }

    @Test
    void invalidDagDoesNotRegisterAnyRuntimeState() {
        ExecutionDefinitionRepository repository = new InMemoryExecutionDefinitionRepository();
        CanvasTriggerApplicationService triggerService = new CanvasTriggerApplicationService();
        CanvasSchedulerApplicationService schedulerService = new CanvasSchedulerApplicationService();
        ExecutionPublicationApplicationService service = new ExecutionPublicationApplicationService(
                new DagRuntimeService(),
                triggerService,
                schedulerService,
                repository);

        PublishedCanvasDefinition cyclic = definition(
                List.of(node("a", "START"), node("b", "SEND_MESSAGE")),
                List.of(edge("a", "b"), edge("b", "a")),
                Map.of("triggerType", "MANUAL"));

        assertThatThrownBy(() -> service.publish(cyclic))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cycle");

        assertThat(repository.findPublished(7L, 11L)).isEmpty();
        assertThat(triggerService.routes()).isEmpty();
        assertThat(schedulerService.registrations()).isEmpty();
    }

    @Test
    void unpublishRemovesDefinitionTriggersAndSchedules() {
        ExecutionDefinitionRepository repository = new InMemoryExecutionDefinitionRepository();
        CanvasTriggerApplicationService triggerService = new CanvasTriggerApplicationService();
        CanvasSchedulerApplicationService schedulerService = new CanvasSchedulerApplicationService();
        ExecutionPublicationApplicationService service = new ExecutionPublicationApplicationService(
                new DagRuntimeService(),
                triggerService,
                schedulerService,
                repository);
        PublishedCanvasDefinition definition = definition(
                List.of(node("start", "START"), node("end", "END")),
                List.of(edge("start", "end")),
                Map.of("triggerType", "MANUAL", "scheduleCron", "0 0 8 * * ?"));

        service.publish(definition);
        service.unpublish(7L, 11L);

        assertThat(repository.findPublished(7L, 11L)).isEmpty();
        assertThat(triggerService.routes()).isEmpty();
        assertThat(schedulerService.registrations()).isEmpty();
    }

    private static PublishedCanvasDefinition definition(
            List<PublishedCanvasNodeDefinition> nodes,
            List<PublishedCanvasEdgeDefinition> edges,
            Map<String, Object> options) {
        return new PublishedCanvasDefinition(
                7L,
                11L,
                13L,
                3,
                "{\"nodes\":[]}",
                Instant.parse("2026-06-10T04:00:00Z"),
                options,
                nodes,
                edges);
    }

    private static PublishedCanvasNodeDefinition node(String nodeId, String nodeType) {
        return new PublishedCanvasNodeDefinition(nodeId, nodeType, nodeType, "{}", Map.of(), Map.of());
    }

    private static PublishedCanvasEdgeDefinition edge(String source, String target) {
        return new PublishedCanvasEdgeDefinition(source + "-" + target, source, target, "{}", Map.of());
    }
}
