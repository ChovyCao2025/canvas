package org.chovy.canvas.engine.channel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.ChannelProviderLimitDO;
import org.chovy.canvas.dal.mapper.ChannelProviderLimitMapper;
import org.springframework.stereotype.Component;

/**
 * ChannelProviderLimitRepository 参与 engine.channel 场景的画布执行引擎处理。
 */
@Component
public class ChannelProviderLimitRepository implements ProviderBackpressureService.LimitRepository {

    private final ChannelProviderLimitMapper mapper;

    /**
     * 创建 ChannelProviderLimitRepository 实例并注入 engine.channel 场景依赖。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ChannelProviderLimitRepository(ChannelProviderLimitMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * find 查询 engine.channel 场景的业务数据。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    @Override
    public ProviderBackpressureService.ProviderLimit find(ProviderBackpressureService.LimitKey key) {
        ChannelProviderLimitDO row = findExact(key.tenantId(), key.channel(), key.provider(), key.operation());
        if (row == null && key.tenantId() != 0L) {
            row = findExact(0L, key.channel(), key.provider(), key.operation());
        }
        if (row == null) {
            return null;
        }
        return new ProviderBackpressureService.ProviderLimit(
                row.getPerSecondLimit(),
                row.getDailyLimit(),
                row.getFailClosed() == null || row.getFailClosed() == 1);
    }

    /**
     * 按租户、渠道、供应商和操作精确查询限流策略。
     *
     * @param tenantId 租户 ID
     * @param channel 渠道标识
     * @param provider 供应商标识
     * @param operation 操作类型
     * @return 匹配的限流记录，未命中时返回 null
     */
    private ChannelProviderLimitDO findExact(Long tenantId, String channel, String provider, String operation) {
        return mapper.selectOne(new LambdaQueryWrapper<ChannelProviderLimitDO>()
                .eq(ChannelProviderLimitDO::getTenantId, tenantId)
                .eq(ChannelProviderLimitDO::getChannel, channel)
                .eq(ChannelProviderLimitDO::getProvider, provider)
                .eq(ChannelProviderLimitDO::getOperation, operation)
                .last("LIMIT 1"));
    }
}
