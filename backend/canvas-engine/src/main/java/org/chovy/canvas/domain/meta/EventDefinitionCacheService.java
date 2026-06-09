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

/**
 * 事件定义 Cache 元数据领域服务。
 *
 * <p>负责事件、接口、标签、系统选项或实验分组等配置型数据的维护和查询。
 * <p>元数据会影响画布运行时行为，因此该层需要兼顾管理端易用性与执行链路缓存一致性。
 */
@Service
@RequiredArgsConstructor
public class EventDefinitionCacheService {
    /** 事件定义 Mapper，用于查询发布态事件元数据。 */
    private final EventDefinitionMapper eventMapper;

    /**
     * 按事件编码读取发布态事件定义，并使用两级缓存保护高频触发查询。
     * 返回 null 会短期缓存，用于防穿透；发布态事件更新后需调用失效方法清理缓存。
     */
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
    /**
     * 执行发布态事件定义的实际查询。
     * 缓存注解会处理空值短期缓存、单飞和 TTL 抖动，返回值供触发器运行时解析事件元数据。
     */
    public EventDefinitionDO getPublishedByCode(String eventCode) {
        return eventMapper.selectOne(new LambdaQueryWrapper<EventDefinitionDO>()
                .eq(EventDefinitionDO::getEventCode, eventCode)
                .eq(EventDefinitionDO::getEnabled, CanvasStatusEnum.PUBLISHED.getCode()));
    }

    /** 提交后驱逐指定事件编码的发布态定义缓存。 */
    @TieredCacheEvict(name = "event-definition-published", key = "#eventCode", afterCommit = true)
    public void invalidatePublishedByCode(String eventCode) {
    }
}
