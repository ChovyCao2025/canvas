package org.chovy.canvas.engine.channel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.ChannelFallbackPolicyDO;
import org.chovy.canvas.dal.mapper.ChannelFallbackPolicyMapper;
import org.springframework.stereotype.Component;

/**
 * ChannelFallbackPolicyRepository 参与 engine.channel 场景的画布执行引擎处理。
 */
@Component
public class ChannelFallbackPolicyRepository implements ChannelFallbackService.PolicyRepository {

    private final ChannelFallbackPolicyMapper mapper;

    /**
     * 创建 ChannelFallbackPolicyRepository 实例并注入 engine.channel 场景依赖。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ChannelFallbackPolicyRepository(ChannelFallbackPolicyMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * find 查询 engine.channel 场景的业务数据。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param channel channel 参数，用于 find 流程中的校验、计算或对象转换。
     * @param provider provider 参数，用于 find 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    @Override
    public ChannelFallbackService.FallbackPolicy find(Long tenantId, String channel, String provider) {
        ChannelFallbackPolicyDO row = findExact(ChannelConnectorRegistry.tenant(tenantId), channel, provider);
        if (row == null && tenantId != null && tenantId != 0L) {
            row = findExact(0L, channel, provider);
        }
        if (row == null) {
            return null;
        }
        return new ChannelFallbackService.FallbackPolicy(
                row.getFallbackChannel(),
                row.getFallbackProvider(),
                row.getEnabled() == null || row.getEnabled() == 1,
                row.getReason());
    }

    /**
     * 按租户、渠道和供应商精确查询降级策略。
     *
     * @param tenantId 租户 ID
     * @param channel 渠道标识
     * @param provider 供应商标识
     * @return 匹配的降级策略记录，未命中时返回 null
     */
    private ChannelFallbackPolicyDO findExact(Long tenantId, String channel, String provider) {
        return mapper.selectOne(new LambdaQueryWrapper<ChannelFallbackPolicyDO>()
                .eq(ChannelFallbackPolicyDO::getTenantId, tenantId)
                .eq(ChannelFallbackPolicyDO::getChannel, channel)
                .eq(ChannelFallbackPolicyDO::getProvider, provider)
                .last("LIMIT 1"));
    }
}
