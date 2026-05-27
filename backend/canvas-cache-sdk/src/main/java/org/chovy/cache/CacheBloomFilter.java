package org.chovy.cache;

/**
 * 二级缓存防穿透布隆过滤器抽象。
 *
 * <p>调用方在读取远端缓存或数据源前先通过该接口判断 key 是否可能存在，减少无效请求继续下沉到 Redis 或数据库。
 * <p>接口只定义判定与登记能力，具体误判率、刷新策略和存储介质由接入方实现。
 */
public interface CacheBloomFilter<K> {
    boolean mightContain(K key);

    void put(K key);
}
