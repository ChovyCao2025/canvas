package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GroovyHandlerValidationTest {

    @Test
    void validate_rules_are_applied_to_script_output() {
        GroovyHandler handler = new GroovyHandler(new GroovyScriptCache());
        handler.init();
        ReflectionTestUtils.setField(handler, "timeoutMs", 1000L);
        ReflectionTestUtils.setField(handler, "maxOutputKb", 64);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setCanvasId(1L);
        ctx.setExecutionId("exec_1");

        NodeResult result = handler.executeAsync(Map.of(
                "code", "return [result: true, score: 5]",
                "validateResult", true,
                "validateRules", List.of(Map.of(
                        "field", "score",
                        "operator", "GT",
                        "value", "10"))), ctx).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("输出校验不通过");
    }
}
