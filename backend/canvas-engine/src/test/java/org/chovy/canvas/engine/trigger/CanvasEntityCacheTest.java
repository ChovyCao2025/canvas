package org.chovy.canvas.engine.trigger;

import org.chovy.cache.testing.InMemoryTieredCache;
import org.chovy.canvas.domain.canvas.Canvas;
import org.chovy.canvas.infra.cache.CanvasEntityCache;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasEntityCacheTest {
    @Test
    void getDeduplicatesLoadsUntilInvalidated() {
        AtomicInteger loads = new AtomicInteger();
        CanvasEntityCache cache = new CanvasEntityCache(InMemoryTieredCache.of("canvas-entity", id -> {
            Canvas canvas = new Canvas();
            canvas.setId(id);
            canvas.setName("load-" + loads.incrementAndGet());
            return canvas;
        }));

        Canvas first = cache.get(10L);
        Canvas second = cache.get(10L);
        cache.invalidate(10L);
        Canvas afterInvalidate = cache.get(10L);

        assertThat(first.getName()).isEqualTo("load-1");
        assertThat(second).isSameAs(first);
        assertThat(afterInvalidate.getName()).isEqualTo("load-2");
        assertThat(loads).hasValue(2);
    }
}
