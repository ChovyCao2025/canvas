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
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;

/**
 * 频控节点处理器。
 *
 * <p>由 DAG 执行器在运行画布节点时调用，读取节点 config 与执行上下文，产出 NodeResult 决定后续路由。
 * <p>处理器应保持单节点职责，跨节点编排、重试和状态持久化由执行引擎统一管理。
 */
@Component
@NodeHandlerType(NodeType.FREQUENCY_CAP)
public class FrequencyCapHandler implements NodeHandler {

    /** 营销策略服务，用于消费并校验用户频控额度。 */
    private final MarketingPolicyService policyService;

    /**
     * 构造 FrequencyCapHandler 实例，并根据入参初始化依赖、配置或内部状态。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param policyService policyService 方法执行所需的业务参数
     */
    public FrequencyCapHandler(MarketingPolicyService policyService) {
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
        return Mono.fromCallable(() -> executeBlocking(config, ctx))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Mono.just(NodeResult.fail("FREQUENCY_CAP: " + e.getMessage())));
    }

    private NodeResult executeBlocking(Map<String, Object> config, ExecutionContext ctx) {
        String nodeId = string(config, "__nodeId", "frequency-cap");
        String scope = string(config, "scope", "JOURNEY");
        String channel = string(config, "channel", "ALL");
        int maxCount = number(config.get("maxCount"), 1);
        Duration window = duration(
                number(config.get("windowValue"), 1),
                string(config, "windowUnit", "DAYS"));
        String passNodeId = string(config, "passNodeId", string(config, "nextNodeId", null));
        String cappedNodeId = string(config, "cappedNodeId", string(config, "suppressedNodeId", null));

        // 频控检查会消费一次计数，因此必须在真正触达前执行。
        MarketingPolicyService.PolicyDecision decision = policyService.consumeFrequency(
                ctx.getUserId(), ctx.getCanvasId(), nodeId, scope, channel, maxCount, window);
        if (!decision.allowed()) {
            // 超出频控时返回 suppressed，调度层可记录抑制原因并走 capped 分支。
            return NodeResult.suppressed(
                    "capped", cappedNodeId, decision.reasonCode(), decision.reasonMessage());
        }
        return NodeResult.routed("pass", passNodeId, Map.of(MapFieldKeys.FREQUENCY_ALLOWED, true));
    }

    /**
     * 执行 number 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param value value 待写入、比较或转换的业务值
     * @param fallback fallback 方法执行所需的业务参数
     * @return 计算得到的数值结果
     */
    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    /**
     * 执行 duration 对应的业务逻辑。
     *
     * <p>执行过程中会根据节点配置和上下文决定成功、失败或下一跳路由。
     *
     * @param amount amount 方法执行所需的业务参数
     * @param unit unit 方法执行所需的业务参数
     * @return 方法执行后的业务结果
     */
    private Duration duration(int amount, String unit) {
        return switch (unit == null ? "DAYS" : unit.toUpperCase()) {
            case "SECOND", "SECONDS" -> Duration.ofSeconds(amount);
            case "MINUTE", "MINUTES" -> Duration.ofMinutes(amount);
            case "HOUR", "HOURS" -> Duration.ofHours(amount);
            default -> Duration.ofDays(amount);
        };
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
