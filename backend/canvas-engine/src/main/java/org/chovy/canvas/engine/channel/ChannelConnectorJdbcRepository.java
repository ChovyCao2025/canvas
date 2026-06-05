package org.chovy.canvas.engine.channel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.ChannelConnectorDO;
import org.chovy.canvas.dal.mapper.ChannelConnectorMapper;
import org.springframework.stereotype.Component;

@Component
public class ChannelConnectorJdbcRepository implements ChannelConnectorRegistry.Repository {

    private final ChannelConnectorMapper mapper;

    public ChannelConnectorJdbcRepository(ChannelConnectorMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ChannelConnectorRegistry.ConnectorConfig find(Long tenantId, String channel, String provider) {
        ChannelConnectorDO row = findExact(ChannelConnectorRegistry.tenant(tenantId), channel, provider);
        if (row == null && tenantId != null && tenantId != 0L) {
            row = findExact(0L, channel, provider);
        }
        if (row == null) {
            return null;
        }
        return new ChannelConnectorRegistry.ConnectorConfig(
                row.getConnectorKey(),
                row.getChannel(),
                row.getProvider(),
                ChannelConnectorRegistry.parseMode(row.getMode()),
                row.getDisabledReason(),
                row.getHealthStatus(),
                row.getHealthMessage(),
                row.getCapabilitiesJson());
    }

    private ChannelConnectorDO findExact(Long tenantId, String channel, String provider) {
        return mapper.selectOne(new LambdaQueryWrapper<ChannelConnectorDO>()
                .eq(ChannelConnectorDO::getTenantId, tenantId)
                .eq(ChannelConnectorDO::getChannel, channel)
                .eq(ChannelConnectorDO::getProvider, provider)
                .last("LIMIT 1"));
    }
}
