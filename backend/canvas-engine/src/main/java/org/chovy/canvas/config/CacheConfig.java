package org.chovy.canvas.config;

import org.chovy.cache.TieredCache;
import org.chovy.cache.TieredCacheBuilder;
import org.chovy.cache.TieredCacheManager;
import org.chovy.cache.strategy.AvalancheProtectionStrategy;
import org.chovy.cache.strategy.BreakdownProtectionStrategy;
import org.chovy.cache.strategy.PenetrationProtectionStrategy;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.dal.mapper.CanvasMapper;
import org.chovy.canvas.dal.mapper.CanvasVersionMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Cache Spring 配置类。
 *
 * <p>负责注册后端运行所需的 Bean、过滤器或基础设施参数，集中管理框架层装配逻辑。
 * <p>业务代码不应直接依赖配置细节，而应通过注入后的组件使用对应能力。
 */
@Configuration
public class CacheConfig {
    @Bean("canvasConfigGraphJsonCache")
    TieredCache<Long, String> canvasConfigGraphJsonCache(TieredCacheManager manager,
                                                         CanvasVersionMapper mapper) {
        return TieredCacheBuilder.<Long, String>builder()
                .name("canvas-config")
                .l1MaxSize(500)
                .l1RefreshAfterWrite(Duration.ofHours(2))
                .l2KeyPrefix("canvas:config:")
                .l2Ttl(Duration.ofHours(24))
                .l2TtlJitter(0.1)
                .nullValueTtl(Duration.ofMinutes(1))
                .penetration(PenetrationProtectionStrategy.CACHE_NULL_SHORT_TTL)
                .breakdown(BreakdownProtectionStrategy.LOCAL_AND_DISTRIBUTED)
                .avalanche(AvalancheProtectionStrategy.FULL)
                .lockTtl(Duration.ofSeconds(30))
                .refreshAhead(Duration.ofHours(1))
                .staleTtl(Duration.ofHours(6))
                .loader(versionId -> {
                    var version = mapper.selectById(versionId);
                    if (version == null) {
                        throw new IllegalArgumentException("版本不存在: " + versionId);
                    }
                    if (version.getGraphJson() == null || version.getGraphJson().isBlank()) {
                        throw new IllegalStateException("版本 " + versionId + " 的 graph_json 已被归档清空，无法加载");
                    }
                    return version.getGraphJson();
                })
                .valueType(String.class)
                .build(manager);
    }

    @Bean("canvasEntityTieredCache")
    TieredCache<Long, CanvasDO> canvasEntityCache(TieredCacheManager manager,
                                                CanvasMapper mapper) {
        return TieredCacheBuilder.<Long, CanvasDO>builder()
                .name("canvas-entity")
                .l1MaxSize(500)
                .l1RefreshAfterWrite(Duration.ofMinutes(5))
                .l2KeyPrefix("canvas:entity:")
                .l2Ttl(Duration.ofMinutes(30))
                .l2TtlJitter(0.1)
                .nullValueTtl(Duration.ofMinutes(1))
                .penetration(PenetrationProtectionStrategy.CACHE_NULL_SHORT_TTL)
                .breakdown(BreakdownProtectionStrategy.LOCAL_SINGLE_FLIGHT)
                .avalanche(AvalancheProtectionStrategy.TTL_JITTER)
                .loader(mapper::selectById)
                .valueType(CanvasDO.class)
                .build(manager);
    }
}
