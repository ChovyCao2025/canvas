package org.chovy.canvas.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 定义 ExecutionContextBoundsTest 的执行上下文数据结构或业务契约。
 */
class ExecutionContextBoundsTest {

    /**
     * 执行 nodeOutputsAreImmutableAndBounded 对应的业务处理。
     */
    @Test
    void nodeOutputsAreImmutableAndBounded() {
        ExecutionContext context = new ExecutionContext("exec-1", 1L, 2L, 64);
        context.putNodeOutput("start", Map.of("ok", true));

        assertThat(context.nodeOutput("start")).containsEntry("ok", true);
        assertThatThrownBy(() -> context.nodeOutputs().put("other", Map.of()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> context.putNodeOutput("large", Map.of("blob", "x".repeat(256))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("context size limit");
    }
}
