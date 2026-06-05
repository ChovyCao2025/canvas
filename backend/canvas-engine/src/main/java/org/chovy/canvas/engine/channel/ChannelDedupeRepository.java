package org.chovy.canvas.engine.channel;

import org.chovy.canvas.dal.dataobject.ChannelDedupeRecordDO;
import org.chovy.canvas.dal.mapper.ChannelDedupeRecordMapper;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class ChannelDedupeRepository implements ChannelDedupeService.Repository {

    private final ChannelDedupeRecordMapper mapper;

    public ChannelDedupeRepository(ChannelDedupeRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean reserve(Long tenantId,
                           String dedupeGroup,
                           String contentHash,
                           String channel,
                           String userId,
                           Duration ttl) {
        ChannelDedupeRecordDO record = new ChannelDedupeRecordDO();
        record.setTenantId(ChannelConnectorRegistry.tenant(tenantId));
        record.setDedupeGroup(dedupeGroup);
        record.setContentHash(contentHash);
        record.setChannel(channel);
        record.setUserId(userId);
        record.setExpiresAt(LocalDateTime.now().plus(ttl));
        return mapper.insertIgnore(record) == 1;
    }
}
