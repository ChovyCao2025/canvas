package org.chovy.canvas.engine.handler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class HandlerRegistry {

    private final List<NodeHandler> handlers;
    private final Map<String, NodeHandler> registry = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        for (NodeHandler handler : handlers) {
            NodeHandlerType anno = AnnotationUtils.findAnnotation(
                    handler.getClass(), NodeHandlerType.class);
            if (anno != null) {
                registry.put(anno.value(), handler);
                log.info("注册节点 Handler: {} → {}", anno.value(),
                        handler.getClass().getSimpleName());
            }
        }
        log.info("HandlerRegistry 初始化完成，共 {} 个 Handler", registry.size());
    }

    public NodeHandler get(String typeKey) {
        NodeHandler h = registry.get(typeKey);
        if (h == null) {
            throw new IllegalStateException(
                    "未注册的节点类型: " + typeKey + "，请检查 @NodeHandlerType 注解或 node_type_registry 表");
        }
        return h;
    }

    public boolean has(String typeKey) {
        return registry.containsKey(typeKey);
    }
}
