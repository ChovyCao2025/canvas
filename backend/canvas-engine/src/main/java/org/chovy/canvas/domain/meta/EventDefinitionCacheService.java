package org.chovy.canvas.domain.meta;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.chovy.cache.annotation.TieredCacheEvict;
import org.chovy.cache.annotation.TieredCached;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventDefinitionCacheService {
    private final EventDefinitionMapper eventMapper;

    @TieredCached(
            name = "event-definition-published",
            key = "#eventCode",
            valueType = EventDefinition.class,
            l1MaxSize = 200,
            l1RefreshAfterWrite = "10m",
            l2KeyPrefix = "canvas:event-definition:",
            l2Ttl = "1h",
            nullValueTtl = "5m")
    public EventDefinition getPublishedByCode(String eventCode) {
        return eventMapper.selectOne(new LambdaQueryWrapper<EventDefinition>()
                .eq(EventDefinition::getEventCode, eventCode)
                .eq(EventDefinition::getEnabled, CanvasStatusEnum.PUBLISHED.getCode()));
    }

    @TieredCacheEvict(name = "event-definition-published", key = "#eventCode", afterCommit = true)
    public void invalidatePublishedByCode(String eventCode) {
    }
}
