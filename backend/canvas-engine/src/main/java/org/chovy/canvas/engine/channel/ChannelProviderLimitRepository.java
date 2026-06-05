package org.chovy.canvas.engine.channel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.ChannelProviderLimitDO;
import org.chovy.canvas.dal.mapper.ChannelProviderLimitMapper;
import org.springframework.stereotype.Component;

@Component
public class ChannelProviderLimitRepository implements ProviderBackpressureService.LimitRepository {

    private final ChannelProviderLimitMapper mapper;

    public ChannelProviderLimitRepository(ChannelProviderLimitMapper mapper) {
        this.mapper = mapper;
    }

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

    private ChannelProviderLimitDO findExact(Long tenantId, String channel, String provider, String operation) {
        return mapper.selectOne(new LambdaQueryWrapper<ChannelProviderLimitDO>()
                .eq(ChannelProviderLimitDO::getTenantId, tenantId)
                .eq(ChannelProviderLimitDO::getChannel, channel)
                .eq(ChannelProviderLimitDO::getProvider, provider)
                .eq(ChannelProviderLimitDO::getOperation, operation)
                .last("LIMIT 1"));
    }
}
