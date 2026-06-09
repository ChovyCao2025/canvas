package org.chovy.canvas.engine.channel;

import org.chovy.canvas.dal.dataobject.ChannelDedupeRecordDO;
import org.chovy.canvas.dal.mapper.ChannelDedupeRecordMapper;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * ChannelDedupeRepository 参与 engine.channel 场景的画布执行引擎处理。
 */
@Component
public class ChannelDedupeRepository implements ChannelDedupeService.Repository {

    private final ChannelDedupeRecordMapper mapper;

    /**
     * 创建 ChannelDedupeRepository 实例并注入 engine.channel 场景依赖。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ChannelDedupeRepository(ChannelDedupeRecordMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * reserve 处理 engine.channel 场景的业务逻辑。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dedupeGroup dedupe group 参数，用于 reserve 流程中的校验、计算或对象转换。
     * @param contentHash content hash 参数，用于 reserve 流程中的校验、计算或对象转换。
     * @param channel channel 参数，用于 reserve 流程中的校验、计算或对象转换。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param ttl ttl 参数，用于 reserve 流程中的校验、计算或对象转换。
     * @return 返回 reserve 的布尔判断结果。
     */
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
