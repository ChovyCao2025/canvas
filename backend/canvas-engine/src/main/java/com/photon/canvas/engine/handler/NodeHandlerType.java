package com.photon.canvas.engine.handler;

import java.lang.annotation.*;
import org.springframework.stereotype.Component;

/**
 * 标记节点 Handler，value 为 node_type_registry 中的 type_key。
 * Spring 容器启动时自动扫描并注入 HandlerRegistry。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface NodeHandlerType {
    String value();
}
