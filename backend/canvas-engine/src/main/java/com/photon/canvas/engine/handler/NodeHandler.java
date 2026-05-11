package com.photon.canvas.engine.handler;

import com.photon.canvas.engine.context.ExecutionContext;
import java.util.Map;

/**
 * 节点 Handler 接口。
 * 实现类用 @NodeHandler("TYPE_KEY") 注解注册，Spring 自动注入 HandlerRegistry。
 */
public interface NodeHandler {

    /**
     * 执行节点业务逻辑。
     *
     * @param config 已完成 CONTEXT 字段解析的节点配置（valueType=CONTEXT 已替换为实际值）
     * @param ctx    执行上下文
     * @return       执行结果，含后继节点 ID 和输出字段
     */
    NodeResult execute(Map<String, Object> config, ExecutionContext ctx);

    /** 是否为权益发放节点（执行成功后设 benefitGranted=true） */
    default boolean isBenefitNode() { return false; }

    /** 是否为触达节点（执行成功后设 userReached=true） */
    default boolean isReachNode() { return false; }
}
