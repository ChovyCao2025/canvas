package org.chovy.canvas.execution.api.node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 定义 NodeMetadataContractTest 的执行上下文数据结构或业务契约。
 */
class NodeMetadataContractTest {

    /**
     * 执行 exposesSchemaAndPortsWithoutMutableRegistryState 对应的业务处理。
     */
    @Test
    void exposesSchemaAndPortsWithoutMutableRegistryState() {
        List<String> inputPorts = new ArrayList<>(List.of("in"));
        List<String> outputPorts = new ArrayList<>(List.of("success"));
        NodeMetadataView view = new NodeMetadataView(
                "message.send",
                "Send Message",
                "Messaging",
                "{\"type\":\"object\"}",
                inputPorts,
                outputPorts,
                "canvas-plugin-message",
                true,
                null);

        inputPorts.add("mutated");
        outputPorts.clear();

        assertThat(view.configSchemaJson()).contains("object");
        assertThat(view.inputPorts()).containsExactly("in");
        assertThat(view.outputPorts()).containsExactly("success");
        assertThatThrownBy(() -> view.inputPorts().add("other"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
