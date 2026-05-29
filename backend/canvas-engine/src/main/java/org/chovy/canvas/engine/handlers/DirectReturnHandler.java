package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 直调返回节点：从上下文 / 自定义值构建同步返回给业务方的数据结构。
 *
 * <p>常见于 DIRECT_CALL 流程尾部，用于把流程内字段映射成业务 API 需要的响应结构。
 * 映射规则通常由前端配置面板维护。
 */
@Component
@NodeHandlerType("DIRECT_RETURN")
public class DirectReturnHandler implements NodeHandler {

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
        // data 每一项描述一个“返回字段映射规则”
        List<Map<String, Object>> data = (List<Map<String, Object>>) config.getOrDefault("data", config.get("bizData"));
        Map<String, Object> result = new HashMap<>();

        if (data != null) {
            for (Map<String, Object> item : data) {
                String name      = string(item.getOrDefault("name", item.get("key")));
                String valueType = string(item.get("valueType"));
                String value     = string(item.get("value"));
                if (name == null || name.isBlank()) {
                    continue;
                }
                // name 作为响应字段名，value 作为字段来源或常量值
                // CONTEXT: 从上下文读取；其他类型按字面值回传
                if ("CONTEXT".equals(valueType) || isContextTemplate(value)) {
                    // 从上下文取值时，value 表示字段 key
                    String contextKey = normalizeContextKey(value);
                    result.put(name, contextKey == null ? null : ctx.getContextValue(contextKey));
                } else {
                    // 非 CONTEXT 视作常量值直接透传
                    result.put(name, value);
                }
            }
        }

        // 终止节点，output 即为返回给调用方的数据
        return Mono.just(NodeResult.terminal(result));
    }

    /**
     * 判断 is Benefit Node 相关的业务数据。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    @Override
    public boolean isBenefitNode() { return false; }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static boolean isContextTemplate(String value) {
        return value != null && value.startsWith("${") && value.endsWith("}");
    }

    private static String normalizeContextKey(String value) {
        if (value == null) {
            return null;
        }
        if (isContextTemplate(value)) {
            return value.substring(2, value.length() - 1);
        }
        return value;
    }
}
