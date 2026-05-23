package org.chovy.canvas.infra.cache;

import org.chovy.cache.TieredCache;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
}
