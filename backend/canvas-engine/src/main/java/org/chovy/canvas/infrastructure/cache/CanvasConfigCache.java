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
    /** 画布版本 DAG JSON 的分层缓存入口。 */
    private final TieredCache<Long, String> graphJsonCache;
    /** DAG 解析器，用于将 graphJson 转换为运行时图。 */
    private final DagParser dagParser;
    /** 已解析 DAG 图本地缓存，避免高频重复解析同一版本内容。 */
    private final Cache<ParsedGraphKey, DagGraph> parsedGraphCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(java.time.Duration.ofHours(2))
            .build();

    public CanvasConfigCache(@Qualifier("canvasConfigGraphJsonCache") TieredCache<Long, String> graphJsonCache,
                             DagParser dagParser) {
        this.graphJsonCache = graphJsonCache;
        this.dagParser = dagParser;
    }

    /**
     * 查询或读取 get 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param versionId versionId 对应的业务主键或标识
     * @return 方法执行后的业务结果
     */
    public DagGraph get(Long canvasId, Long versionId) {
        String graphJson = graphJsonCache.getOrThrow(versionId);
        // 解析缓存把 graphJson 纳入 key，同一 versionId 内容变化后不会复用旧 DagGraph。
        ParsedGraphKey key = new ParsedGraphKey(versionId, graphJson);
        return parsedGraphCache.get(key, ignored -> dagParser.parse(graphJson));
    }

    /**
     * 删除、清理或失效 invalidate 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @param versionId versionId 对应的业务主键或标识
     */
    public void invalidate(Long canvasId, Long versionId) {
        // 先清解析缓存再失效 graphJson；跨实例同步由 TieredCache 的失效链路负责。
        parsedGraphCache.asMap().keySet().removeIf(key -> key.versionId().equals(versionId));
        graphJsonCache.invalidate(versionId);
    }

    /**
     * 删除、清理或失效 evict L1 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param l1Key l1Key 对应的缓存键、配置键或业务键
     */
    public void evictL1(String l1Key) {
        // Compatibility hook for old tests/integrations. SDK invalidation handles L1 + L2 together.
    }

    private record ParsedGraphKey(
            /** 画布版本 ID，用于限定解析缓存归属。 */
            Long versionId,
            /** 参与解析缓存命中的 DAG JSON 内容。 */
            String graphJson
    ) {}
}
