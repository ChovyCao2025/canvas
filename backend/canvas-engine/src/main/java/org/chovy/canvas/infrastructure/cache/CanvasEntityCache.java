package org.chovy.canvas.infrastructure.cache;

import org.chovy.cache.TieredCache;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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
