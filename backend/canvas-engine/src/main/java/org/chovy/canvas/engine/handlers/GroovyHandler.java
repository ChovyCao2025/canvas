package org.chovy.canvas.engine.handlers;

import groovy.lang.Binding;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.expression.ExpressionEngine;
import org.chovy.canvas.engine.expression.GroovyExpressionEngine;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Groovy script node orchestration.
 *
 * <p>The handler owns node config parsing, validation, and routing. Expression
 * compilation, sandboxing, timeouts, and output size limits are delegated to
 * {@link ExpressionEngine}.
 */
@Component
@Slf4j
@NodeHandlerType("GROOVY")
public class GroovyHandler implements NodeHandler {

    private final ExpressionEngine expressionEngine;

    @Autowired
    public GroovyHandler(ExpressionEngine expressionEngine) {
        this.expressionEngine = expressionEngine;
    }

    GroovyHandler(GroovyScriptCache scriptCache) {
        this(new GroovyExpressionEngine(scriptCache));
    }

    /** Kept for legacy unit tests that manually instantiate the handler. */
    void init() {
        // ExpressionEngine initializes its own runtime resources.
    }

    /** 暴露给 CanvasService.publish() 调用预编译 */
    public void precompileScript(Long canvasId, String nodeId, String code) {
        expressionEngine.precompile(canvasId, nodeId, code);
    }

    /** 发布新版本时清除旧编译缓存 */
    public void evictCache(Long canvasId) {
        expressionEngine.evictCanvas(canvasId);
    }

    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String code = (String) config.get("code");
        String nextNodeId = (String) config.get(MapFieldKeys.NEXT_NODE_ID);
        List<Map<String, Object>> inputParams = (List<Map<String, Object>>) config.get(MapFieldKeys.INPUT_PARAMS);

        if (code == null || code.isBlank()) {
            return Mono.just(NodeResult.ok(nextNodeId, Map.of()));
        }

        Map<String, Object> input = new HashMap<>();
        if (inputParams != null) {
            for (Map<String, Object> p : inputParams) {
                String name = (String) p.get("name");
                input.put(name, ctx.getContextValue(name));
            }
        }

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("input", input);
            variables.put("userId", ctx.getUserId());
            variables.put("canvasId", String.valueOf(ctx.getCanvasId()));
            variables.put("executionId", ctx.getExecutionId());
            variables.put("ctx", ctx);

            String nodeId = (String) config.getOrDefault(MapFieldKeys.NODE_ID_INTERNAL, "__groovy__");
            Map<String, Object> output = expressionEngine.execute(ctx.getCanvasId(), nodeId, code, variables);

            Boolean validateResult = (Boolean) config.get("validateResult");
            if (Boolean.TRUE.equals(validateResult)) {
                List<Map<String, Object>> rules =
                        (List<Map<String, Object>>) config.get(MapFieldKeys.VALIDATE_RULES);
                boolean valid = rules == null || rules.isEmpty()
                        ? Boolean.TRUE.equals(output.get("result")) || "true".equals(String.valueOf(output.get("result")))
                        : ConditionEvaluator.allMatch(rules, output);
                if (!valid) {
                    return Mono.just(NodeResult.fail("Groovy 脚本输出校验不通过"));
                }
            }

            return Mono.just(NodeResult.ok(nextNodeId, output));
        } catch (Exception e) {
            log.warn("[GROOVY] 表达式引擎执行异常: {}", e.getMessage());
            return Mono.just(NodeResult.fail(e.getMessage()));
        }
    }

    /**
     * 轻量表达式求值，供其他 Handler（如 AggregateHandler）复用安全沙箱。
     * 调用方自行构建 Binding，表达式应返回 Boolean。
     */
    public Object evaluateExpression(String expression, Binding binding) throws Exception {
        return expressionEngine.evaluate(expression, binding.getVariables());
    }
}
