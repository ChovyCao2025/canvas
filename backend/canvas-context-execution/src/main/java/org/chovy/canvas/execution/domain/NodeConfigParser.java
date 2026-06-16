package org.chovy.canvas.execution.domain;

import java.util.Map;

/**
 * 定义 NodeConfigParser 的执行上下文数据结构或业务契约。
 */
@FunctionalInterface
public interface NodeConfigParser {

    /**
     * 执行 parse 对应的业务处理。
     * @param configJson configJson 参数
     * @param nodeId nodeId 参数
     * @return 处理后的结果
     */
    Map<String, Object> parse(String configJson, String nodeId);

    /**
     * 执行 empty 对应的业务处理。
     * @return 处理后的结果
     */
    static NodeConfigParser empty() {
        return (configJson, nodeId) -> Map.of();
    }
}
