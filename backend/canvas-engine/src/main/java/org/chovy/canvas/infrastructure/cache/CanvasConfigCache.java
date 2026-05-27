package org.chovy.canvas.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.chovy.cache.TieredCache;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Canvas Config Cache 基础设施缓存组件。
 *
 * <p>封装画布运行时常用实体或配置的缓存读写，降低执行链路对数据库的直接压力。
 * <p>该组件提供缓存一致性边界，业务服务只关注读取语义和失效时机。
 */
@Service
public class CanvasConfigCache {
    private final TieredCache<Long, String> graphJsonCache;
    private final DagParser dagParser;
    private final Cache<ParsedGraphKey, DagGraph> parsedGraphCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(java.time.Duration.ofHours(2))
            .build();

    public CanvasConfigCache(@Qualifier("canvasConfigGraphJsonCache") TieredCache<Long, String> graphJsonCache,
                             DagParser dagParser) {
        this.graphJsonCache = graphJsonCache;
        this.dagParser = dagParser;
    }

    public DagGraph get(Long canvasId, Long versionId) {
        String graphJson = graphJsonCache.getOrThrow(versionId);
        ParsedGraphKey key = new ParsedGraphKey(versionId, graphJson);
        return parsedGraphCache.get(key, ignored -> dagParser.parse(graphJson));
    }

    public void invalidate(Long canvasId, Long versionId) {
        parsedGraphCache.asMap().keySet().removeIf(key -> key.versionId().equals(versionId));
        graphJsonCache.invalidate(versionId);
    }

    public void evictL1(String l1Key) {
        // Compatibility hook for old tests/integrations. SDK invalidation handles L1 + L2 together.
    }

    private record ParsedGraphKey(Long versionId, String graphJson) {}
}
