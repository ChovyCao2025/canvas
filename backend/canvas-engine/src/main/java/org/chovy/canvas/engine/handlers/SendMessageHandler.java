package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.domain.content.MarketingContentReleaseService;
import org.chovy.canvas.engine.channel.ChannelConnectorRegistry;
import org.chovy.canvas.engine.channel.ChannelDedupeService;
import org.chovy.canvas.engine.channel.ChannelFallbackService;
import org.chovy.canvas.engine.channel.ProviderBackpressureService;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 面向产品配置的通用消息发送节点。
 *
 * <p>公开节点目录不再暴露渠道特定节点，渠道作为配置值进入处理器；旅程编排侧只看到一个消息动作，
 * 触达层仍会收到明确的渠道信息。
 */
@Component
@NodeHandlerType(NodeType.SEND_MESSAGE)
public class SendMessageHandler extends AbstractSendMessageHandler {

    /**
     * 创建仅使用旧版触达服务的消息发送处理器。
     *
     * @param deliveryService 触达发送服务
     */
    SendMessageHandler(ReachDeliveryService deliveryService) {
        super(deliveryService);
    }

    /**
     * 创建 SendMessageHandler 实例并注入 engine.handlers 场景依赖。
     * @param deliveryService 依赖组件，用于完成数据访问或外部能力调用。
     * @param connectorRegistry connector registry 参数，用于 SendMessageHandler 流程中的校验、计算或对象转换。
     * @param backpressureService 依赖组件，用于完成数据访问或外部能力调用。
     * @param fallbackService 依赖组件，用于完成数据访问或外部能力调用。
     * @param dedupeService 依赖组件，用于完成数据访问或外部能力调用。
     * @param contentReleaseServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public SendMessageHandler(ReachDeliveryService deliveryService,
                              ChannelConnectorRegistry connectorRegistry,
                              ProviderBackpressureService backpressureService,
                              ChannelFallbackService fallbackService,
                              ChannelDedupeService dedupeService,
                              ObjectProvider<MarketingContentReleaseService> contentReleaseServiceProvider) {
        super(deliveryService,
                connectorRegistry,
                backpressureService,
                fallbackService,
                dedupeService,
                contentReleaseServiceProvider == null ? null : contentReleaseServiceProvider.getIfAvailable());
    }

    /**
     * 创建带渠道控制能力的消息发送处理器。
     *
     * @param deliveryService 触达发送服务
     * @param connectorRegistry 渠道连接器注册表
     * @param backpressureService 供应商背压服务
     * @param fallbackService 渠道降级服务
     * @param dedupeService 渠道去重服务
     */
    SendMessageHandler(ReachDeliveryService deliveryService,
                       ChannelConnectorRegistry connectorRegistry,
                       ProviderBackpressureService backpressureService,
                       ChannelFallbackService fallbackService,
                       ChannelDedupeService dedupeService) {
        super(deliveryService, connectorRegistry, backpressureService, fallbackService, dedupeService);
    }

    /**
     * 创建带渠道连接器注册表的消息发送处理器。
     *
     * @param deliveryService 触达发送服务
     * @param connectorRegistry 渠道连接器注册表
     */
    SendMessageHandler(ReachDeliveryService deliveryService, ChannelConnectorRegistry connectorRegistry) {
        super(deliveryService, connectorRegistry);
    }

    /**
     * 创建带内容发布解析服务的消息发送处理器。
     *
     * @param deliveryService 触达发送服务
     * @param contentReleaseService 内容发布解析服务
     */
    SendMessageHandler(ReachDeliveryService deliveryService,
                       MarketingContentReleaseService contentReleaseService) {
        super(deliveryService, contentReleaseService);
    }

    /**
     * 返回默认邮件触达渠道。
     *
     * <p>该值用于发送类基类选择连接器、写入触达流水和计算默认副作用幂等键；下一跳仍由节点配置中的成功/失败路由决定。
     *
     * @return 固定为 {@code EMAIL}
     */
    @Override
    protected String channel() {
        return "EMAIL";
    }

    /**
     * 从节点配置解析触达渠道。
     *
     * <p>优先读取配置中的 {@code channel}，未配置时回退到邮件渠道。解析结果会影响连接器、限流、去重、outbox payload
     * 和上下文输出中的渠道字段。
     *
     * @param config 当前发送节点配置
     * @return 本次触达使用的大写渠道标识
     */
    @Override
    protected String channel(Map<String, Object> config) {
        Object value = config.get("channel");
        return value == null || value.toString().isBlank()
                /**
                 * 执行 channel 流程，围绕 channel 完成校验、计算或结果组装。
                 *
                 * @return 返回 channel 流程生成的业务结果。
                 */
                ? channel()
                : value.toString().trim().toUpperCase();
    }
}
