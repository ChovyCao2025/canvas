package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.cdp.domain.CdpEventLog;
import org.chovy.canvas.cdp.domain.CdpEventRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

/**
 * 定义 MybatisCdpEvent 的持久化访问契约。
 */
@Repository
public class MybatisCdpEventRepository implements CdpEventRepository {

    /**
     * event Log Mapper。
     */
    private final CdpEventLogMapper eventLogMapper;

    /**
     * 持久化转换器。
     */
    private final CdpPersistenceConverter converter;

    /**
     * 创建当前组件实例。
     */
    public MybatisCdpEventRepository(CdpEventLogMapper eventLogMapper, CdpPersistenceConverter converter) {
        this.eventLogMapper = eventLogMapper;
        this.converter = converter;
    }

    /**
     * 执行 existsByMessageId 对应的 CDP 业务操作。
     */
    @Override
    public boolean existsByMessageId(Long tenantId, String messageId) {
        Long count = eventLogMapper.selectCount(new LambdaQueryWrapper<CdpEventLogDO>()
                .eq(CdpEventLogDO::getTenantId, tenantId)
                .eq(CdpEventLogDO::getMessageId, messageId));
        return count != null && count > 0;
    }

    /**
     * 执行 existsByIdempotencyKey 对应的 CDP 业务操作。
     */
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

    /**
     * 保存save。
     */
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
