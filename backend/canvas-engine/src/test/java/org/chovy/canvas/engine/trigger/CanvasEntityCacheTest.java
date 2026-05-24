package org.chovy.canvas.engine.trigger;

import org.chovy.cache.testing.InMemoryTieredCache;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.infrastructure.cache.CanvasEntityCache;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasEntityCacheTest {
    @Test
    void getDeduplicatesLoadsUntilInvalidated() {
        AtomicInteger loads = new AtomicInteger();
        CanvasEntityCache cache = new CanvasEntityCache(InMemoryTieredCache.of("canvas-entity", id -> {
            CanvasDO canvas = new CanvasDO();
            canvas.setId(id);
            canvas.setName("load-" + loads.incrementAndGet());
            return canvas;
        }));

        CanvasDO first = cache.get(10L);
        CanvasDO second = cache.get(10L);
        cache.invalidate(10L);
        CanvasDO afterInvalidate = cache.get(10L);

        assertThat(first.getName()).isEqualTo("load-1");
        assertThat(second).isSameAs(first);
        assertThat(afterInvalidate.getName()).isEqualTo("load-2");
        assertThat(loads).hasValue(2);
    }
}
