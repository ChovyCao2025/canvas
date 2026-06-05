package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.expression.ExpressionEngine;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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

    @Test
    void delegates_script_execution_to_expression_engine() {
        RecordingExpressionEngine expressionEngine = new RecordingExpressionEngine(Map.of("result", true, "score", 12));
        GroovyHandler handler = new GroovyHandler(expressionEngine);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setCanvasId(7L);
        ctx.setExecutionId("exec_7");
        ctx.setUserId("user_7");
        ctx.setContextValue("amount", 99);

        NodeResult result = handler.executeAsync(Map.of(
                "code", "return [result: true, score: amount]",
                "nextNodeId", "next_1",
                "inputParams", List.of(Map.of("name", "amount"))), ctx).block();

        assertThat(result.success()).isTrue();
        assertThat(result.nextNodeId()).isEqualTo("next_1");
        assertThat(result.output()).containsEntry("score", 12);
        assertThat(expressionEngine.canvasId).isEqualTo(7L);
        assertThat(expressionEngine.nodeId).isEqualTo("__groovy__");
        assertThat(expressionEngine.code).isEqualTo("return [result: true, score: amount]");
        assertThat(expressionEngine.variables).containsEntry("userId", "user_7");
        assertThat(expressionEngine.variables).containsEntry("canvasId", "7");
        assertThat(expressionEngine.variables).containsEntry("executionId", "exec_7");
        assertThat((Map<Object, Object>) expressionEngine.variables.get("input")).containsEntry("amount", 99);
    }

    @Test
    void delegates_precompile_and_evict_to_expression_engine() {
        RecordingExpressionEngine expressionEngine = new RecordingExpressionEngine(Map.of());
        GroovyHandler handler = new GroovyHandler(expressionEngine);

        handler.precompileScript(9L, "node_1", "return [result: true]");
        handler.evictCache(9L);

        assertThat(expressionEngine.precompiledCanvasId).isEqualTo(9L);
        assertThat(expressionEngine.precompiledNodeId).isEqualTo("node_1");
        assertThat(expressionEngine.precompiledCode).isEqualTo("return [result: true]");
        assertThat(expressionEngine.evictedCanvasId).isEqualTo(9L);
    }

    private static final class RecordingExpressionEngine implements ExpressionEngine {
        private final Map<String, Object> result;
        private Long canvasId;
        private String nodeId;
        private String code;
        private Map<String, Object> variables = Map.of();
        private Long precompiledCanvasId;
        private String precompiledNodeId;
        private String precompiledCode;
        private Long evictedCanvasId;

        private RecordingExpressionEngine(Map<String, Object> result) {
            this.result = result;
        }

        @Override
        public void precompile(Long canvasId, String nodeId, String code) {
            this.precompiledCanvasId = canvasId;
            this.precompiledNodeId = nodeId;
            this.precompiledCode = code;
        }

        @Override
        public Map<String, Object> execute(Long canvasId, String nodeId, String code, Map<String, Object> variables)
                throws ExpressionException {
            this.canvasId = canvasId;
            this.nodeId = nodeId;
            this.code = code;
            this.variables = new HashMap<>(variables);
            return result;
        }

        @Override
        public Object evaluate(String expression, Map<String, Object> variables) throws ExpressionException {
            return result.get("result");
        }

        @Override
        public void evictCanvas(Long canvasId) {
            this.evictedCanvasId = canvasId;
        }
    }
}
