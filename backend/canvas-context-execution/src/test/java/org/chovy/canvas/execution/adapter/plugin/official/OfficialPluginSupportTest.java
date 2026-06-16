package org.chovy.canvas.execution.adapter.plugin.official;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.chovy.canvas.execution.domain.DagNode;
import org.chovy.canvas.execution.domain.NodeExecutionContext;
import org.junit.jupiter.api.Test;

/**
 * 定义 OfficialPluginSupportTest 的执行上下文数据结构或业务契约。
 */
class OfficialPluginSupportTest {

    /**
     * 执行 trimsStringConfigValues 对应的业务处理。
     */
    @Test
    void trimsStringConfigValues() {
        DagNode node = new DagNode(
                "node-1",
                "official.test",
                "Official Test",
                Map.of("key", " value "),
                Map.of());

        NodeExecutionContext context = new NodeExecutionContext("exec-1", node, Map.of());

        assertThat(OfficialPluginSupport.stringConfig(context, "key")).isEqualTo("value");
    }

    /**
     * 执行 returnsEmptyStringForMissingOrNonStringConfigValues 对应的业务处理。
     */
    @Test
    void returnsEmptyStringForMissingOrNonStringConfigValues() {
        DagNode node = new DagNode(
                "node-1",
                "official.test",
                "Official Test",
                Map.of("count", 3),
                Map.of());

        NodeExecutionContext context = new NodeExecutionContext("exec-1", node, Map.of());

        assertThat(OfficialPluginSupport.stringConfig(context, "missing")).isEmpty();
        assertThat(OfficialPluginSupport.stringConfig(context, "count")).isEmpty();
    }

    /**
     * 执行 defaultsBlankUserIdToAnonymous 对应的业务处理。
     */
    @Test
    void defaultsBlankUserIdToAnonymous() {
        DagNode node = new DagNode("node-1", "official.test", "Official Test", Map.of(), Map.of());

        assertThat(OfficialPluginSupport.userOrAnonymous(new NodeExecutionContext("exec-1", node, Map.of())))
                .isEqualTo("anonymous");
        assertThat(OfficialPluginSupport.userOrAnonymous(new NodeExecutionContext(
                "exec-1",
                node,
                "user-1",
                Map.of(),
                Map.of()))).isEqualTo("user-1");
    }
}
