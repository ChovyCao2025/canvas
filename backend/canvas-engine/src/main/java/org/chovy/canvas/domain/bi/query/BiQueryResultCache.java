package org.chovy.canvas.domain.bi.query;

import java.time.Duration;
import java.util.Optional;

/**
 * BiQueryResultCache 定义 domain.bi.query 场景中的扩展契约。
 */
public interface BiQueryResultCache {

    /**
     * 按 SQL 哈希读取缓存结果。
     *
     * @param sqlHash 编译 SQL、参数和租户上下文生成的缓存键
     * @return 命中的 BI 查询结果；未命中或已过期时为空
     */
    Optional<BiQueryResult> get(String sqlHash);

    /**
     * 写入查询结果缓存，TTL 由具体实现或默认策略决定。
     *
     * @param sqlHash 编译 SQL、参数和租户上下文生成的缓存键
     * @param result 需要缓存的不可变查询结果
     */
    void put(String sqlHash, BiQueryResult result);

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param sqlHash sql hash 参数，用于 put 流程中的校验、计算或对象转换。
     * @param result result 参数，用于 put 流程中的校验、计算或对象转换。
     * @param ttl ttl 参数，用于 put 流程中的校验、计算或对象转换。
     */
    default void put(String sqlHash, BiQueryResult result, Duration ttl) {
        put(sqlHash, result);
    }

    /**
     * 执行 evict 流程，围绕 evict 完成校验、计算或结果组装。
     *
     * @param sqlHash sql hash 参数，用于 evict 流程中的校验、计算或对象转换。
     * @return 返回 evict 的布尔判断结果。
     */
    default boolean evict(String sqlHash) {
        return false;
    }

    /**
     * 执行 evictDataset 流程，围绕 evict dataset 完成校验、计算或结果组装。
     *
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 evict dataset 计算得到的数量、金额或指标值。
     */
    default int evictDataset(String datasetKey) {
        return 0;
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @return 返回 clear 计算得到的数量、金额或指标值。
     */
    default int clear() {
        return 0;
    }

    /**
     * 执行 stats 流程，围绕 stats 完成校验、计算或结果组装。
     *
     * @return 返回 stats 流程生成的业务结果。
     */
    default BiQueryCacheStats stats() {
        return new BiQueryCacheStats("noop", false, 0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * 执行 noop 流程，围绕 noop 完成校验、计算或结果组装。
     *
     * @return 返回 noop 流程生成的业务结果。
     */
    static BiQueryResultCache noop() {
        return new BiQueryResultCache() {
            /**
             * 空缓存实现始终返回未命中。
             *
             * @param sqlHash 查询缓存键
             * @return 始终为空
             */
            @Override
            public Optional<BiQueryResult> get(String sqlHash) {
                return Optional.empty();
            }

            /**
             * 空缓存实现忽略写入请求。
             *
             * @param sqlHash 查询缓存键
             * @param result 查询结果
             */
            @Override
            public void put(String sqlHash, BiQueryResult result) {
            }
        };
    }
}
