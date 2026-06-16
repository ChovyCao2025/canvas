package org.chovy.canvas.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class NodeHandlerRegistryTest {

    @Test
    void returnsHandlersOnlyThroughRegistryAndExposesImmutableMetadata() {
        NodeHandlerRegistry registry = new NodeHandlerRegistry(List.of(new StartHandler(), new EndHandler()));

        NodeHandler handler = registry.handler("START");

        assertThat(handler.execute(new NodeExecutionContext(
                "exec-1",
                new DagNode("start", "START", "Start", Map.of(), Map.of()),
                Map.of())).success()).isTrue();
        assertThat(registry.metadata()).extracting(NodeMetadata::nodeType)
                .containsExactlyInAnyOrder("START", "END");
        assertThatThrownBy(() -> registry.metadata().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void duplicateHandlerTypesFailFast() {
        assertThatThrownBy(() -> new NodeHandlerRegistry(List.of(new StartHandler(), new DuplicateStartHandler())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate node handler type")
                .hasMessageContaining("START");
    }

    @NodeHandlerType("START")
    private static final class StartHandler implements NodeHandler {
        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            return NodeExecutionResult.success(Map.of("started", true));
        }
    }

    @NodeHandlerType("END")
    private static final class EndHandler implements NodeHandler {
        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            return NodeExecutionResult.success(Map.of("ended", true));
        }
    }

    @NodeHandlerType("START")
    private static final class DuplicateStartHandler implements NodeHandler {
        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            return NodeExecutionResult.success(Map.of());
        }
    }
}
