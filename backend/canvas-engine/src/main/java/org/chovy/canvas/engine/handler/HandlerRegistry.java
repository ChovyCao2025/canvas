package org.chovy.canvas.engine.handler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点处理器注册中心。
 * 启动时扫描所有 {@link NodeHandler} 实现，按 {@link NodeHandlerType#value()} 建立索引。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HandlerRegistry {

    /** Spring 注入的全部 NodeHandler 实现。 */
    private final List<NodeHandler> handlers;

    /** type_key -> handler 实例映射。 */
    private final Map<String, NodeHandler> registry = new ConcurrentHashMap<>();

    /** Spring 容器初始化后执行一次，完成类型到处理器的映射。 */
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
        // 若某节点类型未注册，后续 get(typeKey) 会快速失败并提示配置问题
        log.info("HandlerRegistry 初始化完成，共 {} 个 Handler", registry.size());
    }

    /** 按节点类型获取处理器；未注册视为配置错误，直接快速失败。 */
    public NodeHandler get(String typeKey) {
        NodeHandler h = registry.get(typeKey);
        if (h == null) {
            throw new IllegalStateException(
                    "未注册的节点类型: " + typeKey + "，请检查 @NodeHandlerType 注解或 node_type_registry 表");
        }
        return h;
    }

    /**
     * 判断 has 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param typeKey typeKey 对应的缓存键、配置键或业务键
     * @return 判断结果，true 表示校验通过或条件成立
     */
    public boolean has(String typeKey) {
        // 常用于启动检查或兼容分支判断
        return registry.containsKey(typeKey);
    }
}
