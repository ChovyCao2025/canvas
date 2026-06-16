package org.chovy.canvas.execution.domain;

import java.util.Map;

/**
 * 定义 NodeExecutionResult 的执行上下文数据结构或业务契约。
 * @param success success 对应的数据字段
 * @param pending pending 对应的数据字段
 * @param output output 对应的数据字段
 * @param routes routes 对应的数据字段
 * @param error error 对应的数据字段
 */
public record NodeExecutionResult(
        boolean success,
        boolean pending,
        Map<String, Object> output,
        Map<String, String> routes,
        String error) {

    public NodeExecutionResult {
        output = Map.copyOf(output == null ? Map.of() : output);
        routes = Map.copyOf(routes == null ? Map.of() : routes);
        error = error == null ? "" : error;
    }

    public static NodeExecutionResult success(Map<String, Object> output) {
        return new NodeExecutionResult(true, false, output, Map.of(), "");
    }

    public static NodeExecutionResult routed(Map<String, Object> output, Map<String, String> routes) {
        return new NodeExecutionResult(true, false, output, routes, "");
    }

    public static NodeExecutionResult pending(Map<String, Object> output) {
        return new NodeExecutionResult(true, true, output, Map.of(), "");
    }

    public static NodeExecutionResult failure(String error) {
        return new NodeExecutionResult(false, false, Map.of(), Map.of(), error);
    }
}
