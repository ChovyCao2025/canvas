package org.chovy.cache;

/**
 * 二级缓存防穿透布隆过滤器抽象。
 *
 * <p>调用方在读取远端缓存或数据源前先通过该接口判断 key 是否可能存在，减少无效请求继续下沉到 Redis 或数据库。
 * <p>接口只定义判定与登记能力，具体误判率、刷新策略和存储介质由接入方实现。
 */
public interface CacheBloomFilter<K> {
    /**
     * 判断 key 是否可能存在。
     *
     * <p>返回 false 时缓存实现可以直接按穿透保护策略处理，不再访问 Redis 或 L3；返回 true 只表示可能存在。
     *
     * @param key 业务缓存 key
     * @return true 表示 key 可能存在，false 表示 key 一定不存在或应被拦截
     */
    boolean mightContain(K key);

    /**
     * 登记一个已确认存在的 key。
     *
     * <p>通常在数据创建或成功加载后调用，用于降低后续读取被误判为不存在的概率。
     *
     * @param key 业务缓存 key
     */
    void put(K key);
}
