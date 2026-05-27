package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.policy.MarketingPolicyService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 渠道可用性检查节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.CHANNEL_AVAILABILITY)
public class ChannelAvailabilityHandler implements NodeHandler {

    /** 营销策略服务，用于判断用户当前渠道是否可达。 */
    private final MarketingPolicyService policyService;

    /**
     * 构造 ChannelAvailabilityHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param policyService policyService 方法执行所需的业务参数
     */
    public ChannelAvailabilityHandler(MarketingPolicyService policyService) {
        this.policyService = policyService;
    }

    /**
     * 执行当前节点或服务的核心处理流程。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String channel = string(config, "channel", "EMAIL");
        String availableNodeId = string(config, "availableNodeId", string(config, "nextNodeId", null));
        String unavailableNodeId = string(config, "unavailableNodeId", string(config, "suppressedNodeId", null));

        MarketingPolicyService.PolicyDecision decision = policyService.channelAvailable(ctx.getUserId(), channel);
        if (!decision.allowed()) {
            // 渠道不可达不算系统异常，作为抑制结果路由到 unavailable 分支。
            return Mono.just(NodeResult.suppressed(
                    "unavailable", unavailableNodeId, decision.reasonCode(), decision.reasonMessage()));
        }
        return Mono.just(NodeResult.routed("available", availableNodeId, Map.of(MapFieldKeys.CHANNEL_AVAILABLE, true)));
    }

    /**
     * 执行 string 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param config 节点配置或业务配置，方法会从中读取执行参数
     * @param key key 对应的缓存键、配置键或业务键
     * @param fallback fallback 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
