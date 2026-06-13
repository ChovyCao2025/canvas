package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.cdp.domain.CdpEventLog;
import org.chovy.canvas.cdp.domain.CdpEventRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisCdpEventRepository implements CdpEventRepository {

    private final CdpEventLogMapper eventLogMapper;
    private final CdpPersistenceConverter converter;

    public MybatisCdpEventRepository(CdpEventLogMapper eventLogMapper, CdpPersistenceConverter converter) {
        this.eventLogMapper = eventLogMapper;
        this.converter = converter;
    }

    @Override
    public boolean existsByMessageId(Long tenantId, String messageId) {
        Long count = eventLogMapper.selectCount(new LambdaQueryWrapper<CdpEventLogDO>()
                .eq(CdpEventLogDO::getTenantId, tenantId)
                .eq(CdpEventLogDO::getMessageId, messageId));
        return count != null && count > 0;
    }

    @Override
    public boolean existsByIdempotencyKey(Long tenantId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        Long count = eventLogMapper.selectCount(new LambdaQueryWrapper<CdpEventLogDO>()
                .eq(CdpEventLogDO::getTenantId, tenantId)
                .eq(CdpEventLogDO::getIdempotencyKey, idempotencyKey));
        return count != null && count > 0;
    }

    @Override
    public boolean save(CdpEventLog eventLog) {
        try {
            eventLogMapper.insert(converter.toEventRow(eventLog));
            return true;
        } catch (DuplicateKeyException ignored) {
            return false;
        }
    }
}
