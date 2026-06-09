package org.chovy.canvas.engine.channel;

import org.chovy.canvas.dal.dataobject.ChannelFallbackDecisionDO;
import org.chovy.canvas.dal.mapper.ChannelFallbackDecisionMapper;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * ChannelFallbackDecisionRepository 参与 engine.channel 场景的画布执行引擎处理。
 */
@Component
public class ChannelFallbackDecisionRepository implements ChannelFallbackService.DecisionRepository {

    private final ChannelFallbackDecisionMapper mapper;

    /**
     * 创建 ChannelFallbackDecisionRepository 实例并注入 engine.channel 场景依赖。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ChannelFallbackDecisionRepository(ChannelFallbackDecisionMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * insert 处理 engine.channel 场景的业务逻辑。
     * @param decision decision 参数，用于 insert 流程中的校验、计算或对象转换。
     */
    @Override
    public void insert(ChannelFallbackService.FallbackDecision decision) {
        ChannelFallbackDecisionDO row = new ChannelFallbackDecisionDO();
        row.setTenantId(ChannelConnectorRegistry.tenant(decision.tenantId()));
        row.setExecutionId(decision.executionId());
        row.setNodeId(decision.nodeId());
        row.setOriginalChannel(decision.originalChannel());
        row.setOriginalProvider(decision.originalProvider());
        row.setFinalChannel(decision.finalChannel());
        row.setFinalProvider(decision.finalProvider());
        row.setDecisionReason(decision.reason());
        row.setAttemptChainJson(toJsonArray(decision.attemptChain()));
        mapper.insert(row);
    }

    /**
     * 将降级尝试链转换为 JSON 字符串数组。
     *
     * @param values 尝试链条目
     * @return JSON 字符串数组
     */
    private static String toJsonArray(Iterable<String> values) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return "[" + java.util.stream.StreamSupport.stream(values.spliterator(), false)
                .map(value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",")) + "]";
    }
}
