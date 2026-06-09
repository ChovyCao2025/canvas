package org.chovy.canvas.engine.channel;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@Component
/**
 * ChannelFallbackService 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
 */
public class ChannelFallbackService {

    private static final int MAX_CHAIN_DEPTH = 16;

    private final PolicyRepository policies;
    private final DecisionRepository decisions;

    /**
     * 初始化 ChannelFallbackService 实例。
     *
     * @param policies policies 参数，用于 ChannelFallbackService 流程中的校验、计算或对象转换。
     * @param decisions decisions 参数，用于 ChannelFallbackService 流程中的校验、计算或对象转换。
     */
    public ChannelFallbackService(PolicyRepository policies, DecisionRepository decisions) {
        this.policies = Objects.requireNonNull(policies, "policies");
        this.decisions = Objects.requireNonNull(decisions, "decisions");
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param executionId 业务对象 ID，用于定位具体记录。
     * @param nodeId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 resolve 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 resolve 流程中的校验、计算或对象转换。
     * @return 返回 resolve 流程生成的业务结果。
     */
    public FallbackDecision resolve(Long tenantId,
                                    String executionId,
                                    String nodeId,
                                    String channel,
                                    String provider) {
        Route original = Route.of(channel, provider);
        FallbackPolicy policy = policies.find(ChannelConnectorRegistry.tenant(tenantId), original.channel(), original.provider());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (policy == null || !policy.enabled()) {
            return new FallbackDecision(
                    ChannelConnectorRegistry.tenant(tenantId),
                    executionId,
                    nodeId,
                    original.channel(),
                    original.provider(),
                    original.channel(),
                    original.provider(),
                    "NO_FALLBACK_POLICY",
                    List.of(original.key()));
        }
        Route fallback = Route.of(policy.fallbackChannel(), policy.fallbackProvider());
        FallbackDecision decision = new FallbackDecision(
                ChannelConnectorRegistry.tenant(tenantId),
                executionId,
                nodeId,
                original.channel(),
                original.provider(),
                fallback.channel(),
                fallback.provider(),
                policy.reason(),
                List.of(original.key(), fallback.key()));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        decisions.insert(decision);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return decision;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param channel channel 参数，用于 validateNoCycle 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 validateNoCycle 流程中的校验、计算或对象转换。
     */
    public void validateNoCycle(Long tenantId, String channel, String provider) {
        validateChain(ChannelConnectorRegistry.tenant(tenantId), Route.of(channel, provider), null);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param channel channel 参数，用于 validateCandidate 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 validateCandidate 流程中的校验、计算或对象转换。
     * @param fallbackChannel fallback channel 参数，用于 validateCandidate 流程中的校验、计算或对象转换。
     * @param fallbackProvider fallback provider 参数，用于 validateCandidate 流程中的校验、计算或对象转换。
     */
    public void validateCandidate(Long tenantId,
                                  String channel,
                                  String provider,
                                  String fallbackChannel,
                                  String fallbackProvider) {
        FallbackPolicy proposed = new FallbackPolicy(fallbackChannel, fallbackProvider, true, "VALIDATION");
        validateChain(ChannelConnectorRegistry.tenant(tenantId), Route.of(channel, provider), proposed);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param start start 参数，用于 validateChain 流程中的校验、计算或对象转换。
     * @param proposed proposed 参数，用于 validateChain 流程中的校验、计算或对象转换。
     */
    private void validateChain(Long tenantId, Route start, FallbackPolicy proposed) {
        List<Route> chain = new ArrayList<>();
        Route current = start;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int depth = 0; depth < MAX_CHAIN_DEPTH; depth++) {
            int existingIndex = chain.indexOf(current);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (existingIndex >= 0) {
                chain.add(current);
                throw new IllegalArgumentException("fallback cycle: " + joinChain(chain.subList(existingIndex, chain.size())));
            }
            chain.add(current);
            FallbackPolicy policy = findPolicy(tenantId, start, current, proposed);
            if (policy == null || !policy.enabled()) {
                // 汇总前面计算出的状态和明细，返回给调用方。
                return;
            }
            current = Route.of(policy.fallbackChannel(), policy.fallbackProvider());
        }
        throw new IllegalArgumentException("fallback chain exceeds " + MAX_CHAIN_DEPTH + " hops: " + joinChain(chain));
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param start start 参数，用于 findPolicy 流程中的校验、计算或对象转换。
     * @param current current 参数，用于 findPolicy 流程中的校验、计算或对象转换。
     * @param proposed proposed 参数，用于 findPolicy 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private FallbackPolicy findPolicy(Long tenantId, Route start, Route current, FallbackPolicy proposed) {
        if (proposed != null && current.equals(start)) {
            return proposed;
        }
        return policies.find(tenantId, current.channel(), current.provider());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param chain chain 参数，用于 joinChain 流程中的校验、计算或对象转换。
     * @return 返回 join chain 生成的文本或业务键。
     */
    private static String joinChain(List<Route> chain) {
        StringJoiner joiner = new StringJoiner(" -> ");
        for (Route route : chain) {
            joiner.add(route.key());
        }
        return joiner.toString();
    }

    /**
     * PolicyRepository 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public interface PolicyRepository {
        /**
         * 查询并组装符合条件的业务数据。
         *
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param channel channel 参数，用于 find 流程中的校验、计算或对象转换。
         * @param provider provider 参数，用于 find 流程中的校验、计算或对象转换。
         * @return 返回符合条件的数据列表或视图。
         */
        FallbackPolicy find(Long tenantId, String channel, String provider);
    }

    /**
     * DecisionRepository 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public interface DecisionRepository {
        /**
         * 写入或更新业务数据，并保持关联状态一致。
         *
         * @param decision decision 参数，用于 insert 流程中的校验、计算或对象转换。
         */
        void insert(FallbackDecision decision);
    }

    /**
     * FallbackPolicy 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record FallbackPolicy(String fallbackChannel, String fallbackProvider, boolean enabled, String reason) {
    }

    /**
     * FallbackDecision 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    public record FallbackDecision(
            Long tenantId,
            String executionId,
            String nodeId,
            String originalChannel,
            String originalProvider,
            String finalChannel,
            String finalProvider,
            String reason,
            List<String> attemptChain) {
    }

    /**
     * Route 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
    record Route(String channel, String provider) {
        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param channel channel 参数，用于 of 流程中的校验、计算或对象转换。
         * @param provider provider 参数，用于 of 流程中的校验、计算或对象转换。
         * @return 返回 of 流程生成的业务结果。
         */
        static Route of(String channel, String provider) {
            return new Route(
                    ChannelConnectorRegistry.normalize(channel),
                    ChannelConnectorRegistry.normalizeProvider(provider));
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 key 生成的文本或业务键。
         */
        String key() {
            return channel + ":" + provider;
        }
    }
}
