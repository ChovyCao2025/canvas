package org.chovy.canvas.config;

import org.chovy.cache.TieredCache;
import org.chovy.cache.TieredCacheBuilder;
import org.chovy.cache.TieredCacheManager;
import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.domain.canvas.CanvasMapper;
import org.chovy.canvas.domain.canvas.CanvasVersionMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

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
                .nullValueTtl(Duration.ofMinutes(5))
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
    TieredCache<Long, Canvas> canvasEntityCache(TieredCacheManager manager,
                                                CanvasMapper mapper) {
        return TieredCacheBuilder.<Long, Canvas>builder()
                .name("canvas-entity")
                .l1MaxSize(500)
                .l1RefreshAfterWrite(Duration.ofMinutes(5))
                .l2KeyPrefix("canvas:entity:")
                .l2Ttl(Duration.ofMinutes(30))
                .l2TtlJitter(0.1)
                .nullValueTtl(Duration.ofMinutes(2))
                .loader(mapper::selectById)
                .valueType(Canvas.class)
                .build(manager);
    }
}
