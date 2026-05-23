package org.chovy.canvas.infra.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.chovy.cache.TieredCache;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CanvasConfigCache {
    private final TieredCache<Long, String> graphJsonCache;
    private final DagParser dagParser;
    private final Cache<Long, DagGraph> parsedGraphCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(java.time.Duration.ofHours(2))
            .build();

    public CanvasConfigCache(@Qualifier("canvasConfigGraphJsonCache") TieredCache<Long, String> graphJsonCache,
                             DagParser dagParser) {
        this.graphJsonCache = graphJsonCache;
        this.dagParser = dagParser;
    }

    public DagGraph get(Long canvasId, Long versionId) {
        return parsedGraphCache.get(versionId,
                key -> dagParser.parse(graphJsonCache.getOrThrow(key)));
    }

    public void invalidate(Long canvasId, Long versionId) {
        parsedGraphCache.invalidate(versionId);
        graphJsonCache.invalidate(versionId);
    }

    public void evictL1(String l1Key) {
        // Compatibility hook for old tests/integrations. SDK invalidation handles L1 + L2 together.
    }
}
