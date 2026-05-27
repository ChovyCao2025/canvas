package org.chovy.canvas.infrastructure.cache;

import org.chovy.cache.TieredCache;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Canvas Entity Cache 基础设施缓存组件。
 *
 * <p>封装画布运行时常用实体或配置的缓存读写，降低执行链路对数据库的直接压力。
 * <p>该组件提供缓存一致性边界，业务服务只关注读取语义和失效时机。
 */
@Service
public class CanvasEntityCache {
    private final TieredCache<Long, CanvasDO> cache;

    public CanvasEntityCache(@Qualifier("canvasEntityTieredCache") TieredCache<Long, CanvasDO> cache) {
        this.cache = cache;
    }

    public CanvasDO get(Long canvasId) {
        return cache.get(canvasId).orElse(null);
    }

    public void invalidate(Long canvasId) {
        cache.invalidate(canvasId);
    }
}
