package org.chovy.canvas.infrastructure.cache;

import org.chovy.cache.TieredCache;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Canvas Config Cache 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class CanvasConfigCacheTest {

    @Test
    void getCachesParsedDagGraphByVersionId() {
        @SuppressWarnings("unchecked")
        TieredCache<Long, String> graphJsonCache = mock(TieredCache.class);
        DagParser dagParser = mock(DagParser.class);
        DagGraph graph = mock(DagGraph.class);
        String graphJson = "{\"nodes\":[]}";

        when(graphJsonCache.getOrThrow(100L)).thenReturn(graphJson);
        when(dagParser.parse(graphJson)).thenReturn(graph);

        CanvasConfigCache cache = new CanvasConfigCache(graphJsonCache, dagParser);

        DagGraph first = cache.get(1L, 100L);
        DagGraph second = cache.get(1L, 100L);

        assertThat(first).isSameAs(graph);
        assertThat(second).isSameAs(graph);
        verify(dagParser, times(1)).parse(graphJson);
    }

    @Test
    void invalidateClearsParsedDagGraph() {
        @SuppressWarnings("unchecked")
        TieredCache<Long, String> graphJsonCache = mock(TieredCache.class);
        DagParser dagParser = mock(DagParser.class);
        DagGraph firstGraph = mock(DagGraph.class);
        DagGraph secondGraph = mock(DagGraph.class);
        String graphJson = "{\"nodes\":[]}";

        when(graphJsonCache.getOrThrow(100L)).thenReturn(graphJson);
        when(dagParser.parse(graphJson)).thenReturn(firstGraph, secondGraph);

        CanvasConfigCache cache = new CanvasConfigCache(graphJsonCache, dagParser);
        cache.get(1L, 100L);
        cache.invalidate(1L, 100L);
        DagGraph afterInvalidate = cache.get(1L, 100L);

        assertThat(afterInvalidate).isSameAs(secondGraph);
        verify(graphJsonCache).invalidate(100L);
        verify(dagParser, times(2)).parse(graphJson);
    }

    @Test
    void getReparsesWhenGraphJsonChangesForSameVersionId() {
        @SuppressWarnings("unchecked")
        TieredCache<Long, String> graphJsonCache = mock(TieredCache.class);
        DagParser dagParser = mock(DagParser.class);
        DagGraph firstGraph = mock(DagGraph.class);
        DagGraph secondGraph = mock(DagGraph.class);
        String firstJson = "{\"nodes\":[{\"id\":\"a\"}]}";
        String secondJson = "{\"nodes\":[{\"id\":\"b\"}]}";

        when(graphJsonCache.getOrThrow(100L)).thenReturn(firstJson, secondJson);
        when(dagParser.parse(firstJson)).thenReturn(firstGraph);
        when(dagParser.parse(secondJson)).thenReturn(secondGraph);

        CanvasConfigCache cache = new CanvasConfigCache(graphJsonCache, dagParser);

        assertThat(cache.get(1L, 100L)).isSameAs(firstGraph);
        assertThat(cache.get(1L, 100L)).isSameAs(secondGraph);
        verify(dagParser).parse(firstJson);
        verify(dagParser).parse(secondJson);
    }
}
