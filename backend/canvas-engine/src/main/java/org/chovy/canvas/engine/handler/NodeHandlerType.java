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
    /** 对应 `node_type_registry.type_key`，如 `API_CALL` / `IF_CONDITION`。 */
    String value();

    // 注意：仅声明类型，不会自动注册 Bean。
    // 注册 Bean 仍需在实现类显式添加 @Component。
    // HandlerRegistry 初始化时会读取该注解建立 typeKey -> handler 映射。
    // 注解值建议与 node_type_registry.type_key 完全同名，避免映射歧义。
}
