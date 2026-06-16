package org.chovy.canvas.execution.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

@Component
public class NodeHandlerRegistry {

    private final Map<String, NodeHandler> handlers;
    private final List<NodeMetadata> metadata;

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

    public NodeHandler handler(String nodeType) {
        NodeHandler handler = handlers.get(nodeType);
        if (handler == null) {
            throw new IllegalStateException("unregistered node handler type: " + nodeType);
        }
        return handler;
    }

    public boolean has(String nodeType) {
        return handlers.containsKey(nodeType);
    }

    public List<NodeMetadata> metadata() {
        return metadata;
    }
}
