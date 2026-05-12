package org.chovy.canvas.engine.handler;

import java.lang.annotation.*;

/**
 * 标识节点 Handler 的 type_key（对应 node_type_registry.type_key）。
 *
 * 职责拆分说明：
 *   - 本注解仅负责"标识节点类型"，不再兼任 @Component。
 *   - Handler 实现类须自行添加 @Component（或 @Service）注册为 Spring Bean。
 *   - 这样可支持"仅标识类型、但不注册为 Spring Bean"的场景（如单元测试 mock）。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NodeHandlerType {
    String value();
}
