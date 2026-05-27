package org.chovy.canvas.infrastructure.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.ApiDefinitionDO;
import org.chovy.canvas.dal.mapper.ApiDefinitionMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 接口定义 Cache 基础设施缓存组件。
 *
 * <p>封装画布运行时常用实体或配置的缓存读写，降低执行链路对数据库的直接压力。
 * <p>该组件提供缓存一致性边界，业务服务只关注读取语义和失效时机。
 */
@Service
@RequiredArgsConstructor
public class ApiDefinitionCache {

    /** 接口定义数据访问组件，用于按 apiKey 加载启用接口。 */
    private final ApiDefinitionMapper mapper;
    /** apiKey 到启用接口定义的本地缓存，Optional 用于缓存未命中结果。 */
    private final Cache<String, Optional<ApiDefinitionDO>> cache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    /**
     * 查询或读取 get Enabled 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param apiKey apiKey 对应的缓存键、配置键或业务键
     * @return 方法执行后的业务结果
     */
    public ApiDefinitionDO getEnabled(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) return null;
        // Optional 也缓存，避免不存在或未启用的 apiKey 在短时间内反复打到数据库。
        return cache.get(apiKey, this::load).orElse(null);
    }

    /**
     * 删除、清理或失效 invalidate 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param apiKey apiKey 对应的缓存键、配置键或业务键
     */
    public void invalidate(String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            cache.invalidate(apiKey);
        }
    }

    /**
     * 删除、清理或失效 invalidate All 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    public void invalidateAll() {
        cache.invalidateAll();
    }

    /**
     * 查询或读取 load 相关的业务数据。
     *
     * <p>实现会通过持久化层读取或写入数据库记录。
     *
     * @param apiKey apiKey 对应的缓存键、配置键或业务键
     * @return 可能存在的查询结果，未命中或无数据时为空
     */
    private Optional<ApiDefinitionDO> load(String apiKey) {
        // 只缓存 enabled=1 的接口定义，禁用接口按未命中处理，避免执行链路误调用。
        return Optional.ofNullable(mapper.selectOne(
                new LambdaQueryWrapper<ApiDefinitionDO>()
                        .eq(ApiDefinitionDO::getApiKey, apiKey)
                        .eq(ApiDefinitionDO::getEnabled, 1)
        ));
    }
}
