package org.chovy.canvas.infra.cache;

import org.chovy.cache.TieredCache;
import org.chovy.canvas.domain.canvas.Canvas;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CanvasEntityCache {
    private final TieredCache<Long, Canvas> cache;

    public CanvasEntityCache(@Qualifier("canvasEntityTieredCache") TieredCache<Long, Canvas> cache) {
        this.cache = cache;
    }

    public Canvas get(Long canvasId) {
        return cache.get(canvasId).orElse(null);
    }

    public void invalidate(Long canvasId) {
        cache.invalidate(canvasId);
    }
}
