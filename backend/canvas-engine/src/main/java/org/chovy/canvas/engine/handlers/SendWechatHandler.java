package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;

/**
 * 微信发送节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.SEND_WECHAT)
public class SendWechatHandler extends AbstractSendMessageHandler {
    /**
     * 构造 SendWechatHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param deliveryService deliveryService 方法执行所需的业务参数
     */
    public SendWechatHandler(ReachDeliveryService deliveryService) {
        super(deliveryService);
    }

    /**
     * 执行 channel 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @return 转换或查询得到的字符串结果
     */
    @Override
    protected String channel() {
        return "WECHAT";
    }
}
