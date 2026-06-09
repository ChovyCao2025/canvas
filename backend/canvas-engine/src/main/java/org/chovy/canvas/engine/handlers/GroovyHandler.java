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
 * Groovy 脚本节点编排处理器。
 *
 * <p>处理器负责节点配置解析、校验和路由；表达式编译、沙箱、超时和输出大小限制委托给
 * {@link ExpressionEngine}。
 */
@Component
@Slf4j
@NodeHandlerType("GROOVY")
public class GroovyHandler implements NodeHandler {

    private final ExpressionEngine expressionEngine;

    /**
     * 创建 GroovyHandler 实例并注入 engine.handlers 场景依赖。
     * @param expressionEngine expression engine 参数，用于 GroovyHandler 流程中的校验、计算或对象转换。
     */
    @Autowired
    public GroovyHandler(ExpressionEngine expressionEngine) {
        this.expressionEngine = expressionEngine;
    }

    /**
     * 使用脚本缓存创建兼容旧测试的 Groovy 处理器。
     *
     * @param scriptCache Groovy 脚本缓存
     */
    GroovyHandler(GroovyScriptCache scriptCache) {
        this(new GroovyExpressionEngine(scriptCache));
    }

    /** 保留给手动实例化处理器的旧单元测试。 */
    void init() {
        // ExpressionEngine 会自行初始化运行时资源。
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
     * 根据配置参数构造脚本输入，执行 Groovy 表达式并按需校验输出后路由。
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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
