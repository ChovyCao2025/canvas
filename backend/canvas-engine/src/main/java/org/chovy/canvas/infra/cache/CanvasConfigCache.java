package org.chovy.canvas.infra.cache;

import org.chovy.cache.TieredCache;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CanvasConfigCache {
    private final TieredCache<Long, String> graphJsonCache;
    private final DagParser dagParser;

    public CanvasConfigCache(@Qualifier("canvasConfigGraphJsonCache") TieredCache<Long, String> graphJsonCache,
                             DagParser dagParser) {
        this.graphJsonCache = graphJsonCache;
        this.dagParser = dagParser;
    }

    public DagGraph get(Long canvasId, Long versionId) {
        return dagParser.parse(graphJsonCache.getOrThrow(versionId));
    }

    public void invalidate(Long canvasId, Long versionId) {
        graphJsonCache.invalidate(versionId);
    }

    public void evictL1(String l1Key) {
        // Compatibility hook for old tests/integrations. SDK invalidation handles L1 + L2 together.
    }
}
