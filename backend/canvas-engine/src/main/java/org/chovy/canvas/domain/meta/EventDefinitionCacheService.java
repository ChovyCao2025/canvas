package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.cache.annotation.TieredCacheEvict;
import org.chovy.cache.annotation.TieredCached;
import org.chovy.cache.strategy.AvalancheProtectionStrategy;
import org.chovy.cache.strategy.BreakdownProtectionStrategy;
import org.chovy.cache.strategy.PenetrationProtectionStrategy;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.springframework.stereotype.Service;
import org.chovy.canvas.dal.dataobject.EventDefinitionDO;
import org.chovy.canvas.dal.mapper.EventDefinitionMapper;

@Service
@RequiredArgsConstructor
public class EventDefinitionCacheService {
    private final EventDefinitionMapper eventMapper;

    @TieredCached(
            name = "event-definition-published",
            key = "#eventCode",
            valueType = EventDefinitionDO.class,
            l1MaxSize = 200,
            l1RefreshAfterWrite = "10m",
            l2KeyPrefix = "canvas:event-definition:",
            l2Ttl = "1h",
            nullValueTtl = "1m",
            penetration = PenetrationProtectionStrategy.CACHE_NULL_SHORT_TTL,
            breakdown = BreakdownProtectionStrategy.LOCAL_SINGLE_FLIGHT,
            avalanche = AvalancheProtectionStrategy.TTL_JITTER)
    public EventDefinitionDO getPublishedByCode(String eventCode) {
        return eventMapper.selectOne(new LambdaQueryWrapper<EventDefinitionDO>()
                .eq(EventDefinitionDO::getEventCode, eventCode)
                .eq(EventDefinitionDO::getEnabled, CanvasStatusEnum.PUBLISHED.getCode()));
    }

    @TieredCacheEvict(name = "event-definition-published", key = "#eventCode", afterCommit = true)
    public void invalidatePublishedByCode(String eventCode) {
    }
}
