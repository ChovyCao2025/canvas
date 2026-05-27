package org.chovy.canvas.engine.trigger;

import org.chovy.cache.testing.InMemoryTieredCache;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.chovy.canvas.infrastructure.cache.CanvasEntityCache;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Canvas Entity Cache 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
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
