package org.chovy.canvas.execution.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

/**
 * 定义 NodeHandlerRegistry 的执行上下文数据结构或业务契约。
 */
@Component
public class NodeHandlerRegistry {

    /**
     * 保存 Map<String 对应的状态或配置。
     */
    private final Map<String, NodeHandler> handlers;

    /**
     * 保存 metadata 对应的状态或配置。
     */
    private final List<NodeMetadata> metadata;

    /**
     * 执行 NodeHandlerRegistry 对应的业务处理。
     * @param handlers handlers 参数
     */
    public NodeHandlerRegistry(List<NodeHandler> handlers) {
        Map<String, NodeHandler> registered = new LinkedHashMap<>();
        List<NodeMetadata> metadataViews = new ArrayList<>();
        for (NodeHandler handler : handlers == null ? List.<NodeHandler>of() : handlers) {
            NodeHandlerType annotation = AnnotationUtils.findAnnotation(handler.getClass(), NodeHandlerType.class);
            if (annotation == null || annotation.value().isBlank()) {
                throw new IllegalStateException("node handler is missing @NodeHandlerType: "
                        + handler.getClass().getName());
            }
            NodeHandler previous = registered.putIfAbsent(annotation.value(), handler);
            if (previous != null) {
                throw new IllegalStateException("duplicate node handler type: " + annotation.value());
            }
            metadataViews.add(new NodeMetadata(
                    annotation.value(),
                    annotation.value(),
                    "Runtime",
                    "{}",
                    List.of("in"),
                    List.of("success"),
                    "",
                    true,
                    ""));
        }
        this.handlers = Map.copyOf(registered);
        this.metadata = List.copyOf(metadataViews);
    }

    /**
     * 执行 handler 对应的业务处理。
     * @param nodeType nodeType 参数
     * @return 处理后的结果
     */
    public NodeHandler handler(String nodeType) {
        NodeHandler handler = handlers.get(nodeType);
        if (handler == null) {
            throw new IllegalStateException("unregistered node handler type: " + nodeType);
        }
        return handler;
    }

    /**
     * 执行 has 对应的业务处理。
     * @param nodeType nodeType 参数
     * @return 处理后的结果
     */
    public boolean has(String nodeType) {
        return handlers.containsKey(nodeType);
    }

    /**
     * 执行 metadata 对应的业务处理。
     * @return 处理后的结果
     */
    public List<NodeMetadata> metadata() {
        return metadata;
    }
}
