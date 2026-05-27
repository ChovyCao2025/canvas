package org.chovy.canvas.infrastructure.cache;

import org.chovy.cache.TieredCache;
import org.chovy.canvas.dal.dataobject.CanvasDO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Canvas Entity Cache 基础设施缓存组件。
 *
 * <p>封装画布运行时常用实体或配置的缓存读写，降低执行链路对数据库的直接压力。
 * <p>该组件提供缓存一致性边界，业务服务只关注读取语义和失效时机。
 */
@Service
public class CanvasEntityCache {
    /** 画布实体的分层缓存入口。 */
    private final TieredCache<Long, CanvasDO> cache;

    public CanvasEntityCache(@Qualifier("canvasEntityTieredCache") TieredCache<Long, CanvasDO> cache) {
        this.cache = cache;
    }

    /**
     * 查询或读取 get 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     * @return 方法执行后的业务结果
     */
    public CanvasDO get(Long canvasId) {
        // TieredCache 内部负责 L1/L2/加载器编排；这里保持实体读取语义为“未命中返回 null”。
        return cache.get(canvasId).orElse(null);
    }

    /**
     * 删除、清理或失效 invalidate 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param canvasId canvasId 对应的业务主键或标识
     */
    public void invalidate(Long canvasId) {
        // 统一走 TieredCache 失效入口，确保本地缓存和跨节点失效事件保持同一语义。
        cache.invalidate(canvasId);
    }
}
