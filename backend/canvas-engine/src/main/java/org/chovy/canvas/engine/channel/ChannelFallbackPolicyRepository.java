package org.chovy.canvas.engine.channel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.ChannelFallbackPolicyDO;
import org.chovy.canvas.dal.mapper.ChannelFallbackPolicyMapper;
import org.springframework.stereotype.Component;

@Component
public class ChannelFallbackPolicyRepository implements ChannelFallbackService.PolicyRepository {

    private final ChannelFallbackPolicyMapper mapper;

    public ChannelFallbackPolicyRepository(ChannelFallbackPolicyMapper mapper) {
        this.mapper = mapper;
    }

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

    private ChannelFallbackPolicyDO findExact(Long tenantId, String channel, String provider) {
        return mapper.selectOne(new LambdaQueryWrapper<ChannelFallbackPolicyDO>()
                .eq(ChannelFallbackPolicyDO::getTenantId, tenantId)
                .eq(ChannelFallbackPolicyDO::getChannel, channel)
                .eq(ChannelFallbackPolicyDO::getProvider, provider)
                .last("LIMIT 1"));
    }
}
