package org.chovy.cache;

/**
 * 二级缓存防穿透布隆过滤器抽象。
 *
 * <p>调用方在读取远端缓存或数据源前先通过该接口判断 key 是否可能存在，减少无效请求继续下沉到 Redis 或数据库。
 * <p>接口只定义判定与登记能力，具体误判率、刷新策略和存储介质由接入方实现。
 */
public interface CacheBloomFilter<K> {
    /**
     * 执行 might Contain 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     * @return 判断结果，true 表示校验通过或条件成立
     */
    boolean mightContain(K key);

    /**
     * 写入或记录 put 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     */
    void put(K key);
}
