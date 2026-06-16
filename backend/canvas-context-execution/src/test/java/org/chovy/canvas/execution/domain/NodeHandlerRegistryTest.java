package org.chovy.canvas.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 定义 NodeHandlerRegistryTest 的执行上下文数据结构或业务契约。
 */
class NodeHandlerRegistryTest {

    /**
     * 执行 returnsHandlersOnlyThroughRegistryAndExposesImmutableMetadata 对应的业务处理。
     */
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

    /**
     * 执行 duplicateHandlerTypesFailFast 对应的业务处理。
     */
    @Test
    void duplicateHandlerTypesFailFast() {
        assertThatThrownBy(() -> new NodeHandlerRegistry(List.of(new StartHandler(), new DuplicateStartHandler())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate node handler type")
                .hasMessageContaining("START");
    }

    /**
     * 定义 StartHandler 的执行上下文数据结构或业务契约。
     */
    @NodeHandlerType("START")
    private static final class StartHandler implements NodeHandler {
        /**
         * 执行 execute 对应的业务处理。
         * @param context context 参数
         * @return 处理后的结果
         */
        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            return NodeExecutionResult.success(Map.of("started", true));
        }
    }

    /**
     * 定义 EndHandler 的执行上下文数据结构或业务契约。
     */
    @NodeHandlerType("END")
    private static final class EndHandler implements NodeHandler {
        /**
         * 执行 execute 对应的业务处理。
         * @param context context 参数
         * @return 处理后的结果
         */
        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            return NodeExecutionResult.success(Map.of("ended", true));
        }
    }

    /**
     * 定义 DuplicateStartHandler 的执行上下文数据结构或业务契约。
     */
    @NodeHandlerType("START")
    private static final class DuplicateStartHandler implements NodeHandler {
        /**
         * 执行 execute 对应的业务处理。
         * @param context context 参数
         * @return 处理后的结果
         */
        @Override
        public NodeExecutionResult execute(NodeExecutionContext context) {
            return NodeExecutionResult.success(Map.of());
        }
    }
}
