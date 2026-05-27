package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Groovy Handler Validation 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
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
