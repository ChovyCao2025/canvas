# 多级缓存 SDK 设计（优化点 #9）

## 背景

`CanvasConfigCache` 实现了 L1(Caffeine) + L2(Redis) + L3(MySQL) + Pub/Sub 失效广播的三级缓存模式，但强耦合于业务（`CanvasVersionMapper`、`DagGraph`）。若其他业务场景（标签缓存、API 定义缓存等）需要同样模式，只能拷贝重写。

本 SDK 将该模式**泛型化、零业务依赖地抽离为独立 Maven 模块**，任意服务 3 行代码即可接入。

---

## Maven 模块结构

```
backend/
├── pom.xml                          ← 新增：聚合父 pom
├── canvas-cache-sdk/                ← 新增：SDK 模块
│   └── src/main/java/org/chovy/cache/
│       ├── TieredCache.java         ← 核心接口
│       ├── TieredCacheImpl.java     ← 实现
│       ├── TieredCacheBuilder.java  ← Fluent Builder
│       ├── TieredCacheManager.java  ← Spring Bean，管理 Pub/Sub
│       ├── TieredCacheProperties.java
│       └── autoconfigure/
│           └── TieredCacheAutoConfiguration.java
└── canvas-engine/                   ← 现有模块，添加 SDK 依赖
```

SDK 依赖（**无业务代码**）：

```xml
<dependencies>
    <dependency>spring-boot-starter-data-redis</dependency>
    <dependency>caffeine</dependency>
    <dependency>jackson-databind</dependency>
    <dependency>slf4j-api</dependency>
</dependencies>
```

---

## 核心 API 设计

### TieredCache\<K, V\>（接口）

```java
public interface TieredCache<K, V> {
    /** L1 → L2 → Loader 三级查找，结果自动回填上级 */
    Optional<V> get(K key);

    /** get() + 未命中时抛出 NoSuchElementException */
    V getOrThrow(K key);

    /** 写入 L1 + L2（主动 put，绕过 Loader） */
    void put(K key, V value);

    /** 清除 L1 + L2，Pub/Sub 广播给所有实例 */
    void invalidate(K key);

    /** 强制从 Loader 重新加载，更新 L1 + L2 */
    void refresh(K key);

    /** 缓存名称（用于 Pub/Sub channel 隔离） */
    String name();
}
```

### TieredCacheBuilder\<K, V\>（Fluent Builder）

```java
TieredCache<Long, DagGraph> cache = TieredCache.<Long, DagGraph>builder()
    .name("canvas-config")                     // 缓存名，隔离 Pub/Sub channel
    .l1MaxSize(500)                            // L1 最大条数
    .l1RefreshAfterWrite(Duration.ofHours(2))  // L1 续期触发时间（查 L2，不穿透 L3）
    .l2KeyPrefix("canvas:config:")             // Redis key 前缀
    .l2Ttl(Duration.ofHours(24))              // Redis TTL
    .loader(key -> loadFromDb(key))            // L3 数据源（Function<K, V>）
    .valueType(DagGraph.class)                 // Jackson 反序列化目标类型
    .build(cacheManager);                      // 注册到 TieredCacheManager
```

### L1 续期机制（关键设计）

Caffeine 使用 `refreshAfterWrite`（而非 `expireAfterWrite`）：
- L1 条目到达 refresh 时间后，**下次 get() 时异步刷新**
- 刷新逻辑：**先查 L2**，命中则从 L2 续期（不碰 L3）；L2 也过期才调 Loader
- 效果：L1 永远保持热数据，L3 穿透频率大幅降低

```java
// 刷新逻辑（注册到 Caffeine CacheLoader）
V refresh(K key, V oldValue) {
    // 1. 查 L2
    String json = redis.get(l2Key(key));
    if (json != null) {
        V v = deserialize(json);
        // L2 命中，续期 L2 TTL，更新 L1
        redis.expire(l2Key(key), l2Ttl);
        return v;
    }
    // 2. L2 也过期，调 Loader（穿透 L3）
    V v = loader.apply(key);
    redis.set(l2Key(key), serialize(v), l2Ttl);
    return v;
}
```

### 失效广播

```
调用 invalidate(key)
    → 删除本实例 L1
    → 删除 L2 Redis key
    → PUBLISH tiered-cache:{name}:invalidate {key}
         ↓ 所有实例订阅
    → 各实例收到消息，删自己的 L1 Caffeine
```

Channel 按 `cacheName` 隔离，不同缓存互不干扰。

---

## Spring Boot AutoConfiguration

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册：

```
org.chovy.cache.autoconfigure.TieredCacheAutoConfiguration
```

自动创建 `TieredCacheManager` Bean，自动订阅 `tiered-cache:*:invalidate` 通配频道（Redis PSUBSCRIBE）。

### 使用方式（业务服务）

```java
@Configuration
@EnableTieredCache      // 开启 SDK（等价于 @Import(TieredCacheAutoConfiguration.class)）
public class CacheConfig {

    @Bean
    TieredCache<Long, DagGraph> canvasConfigCache(TieredCacheManager manager,
                                                   CanvasVersionMapper mapper,
                                                   DagParser parser) {
        return TieredCache.<Long, DagGraph>builder()
                .name("canvas-config")
                .l1MaxSize(500)
                .l1RefreshAfterWrite(Duration.ofHours(2))
                .l2KeyPrefix("canvas:config:")
                .l2Ttl(Duration.ofHours(24))
                .loader(id -> {
                    CanvasVersion v = mapper.selectById(id);
                    return parser.parse(v.getGraphJson());
                })
                .valueType(DagGraph.class)
                .build(manager);
    }
}
```

---

## 迁移 CanvasConfigCache

迁移后 `CanvasConfigCache` 变为薄包装层（约 30 行），核心逻辑全部委托给 SDK：

```java
@Service
public class CanvasConfigCache {
    private final TieredCache<Long, DagGraph> cache;
    
    public DagGraph get(Long canvasId, Long versionId) {
        return cache.get(versionId).orElseThrow(...);
    }
    
    public void invalidate(Long canvasId, Long versionId) {
        cache.invalidate(versionId);
    }
}
```

---

---

## 缓存三大问题防护

### 缓存穿透（Cache Penetration）

**问题**：查询不存在的 key，Loader 返回 null，不写 L2，每次请求都穿透到 L3。

**防护**：空值缓存（Null Sentinel）。Loader 返回 null 时向 L2 写入哨兵 `__NULL__`，TTL 使用更短的 `nullValueTtl`（默认 5 分钟）。后续请求读到哨兵直接返回 `Optional.empty()`，不再调用 Loader。

Builder 配置：`.nullValueTtl(Duration.ofMinutes(5))`（默认开启）

### 缓存击穿（Cache Breakdown / Hotspot）

**问题**：热点 key 的 L1 被驱逐且 L2 同时过期，多个实例并发穿透 L3。

**Caffeine 的保证**：`LoadingCache` 在单实例内对同一 key 只执行一次 load，其他线程等待结果。**分布式场景无法覆盖**。

**防护**：L2 miss 时通过 Redis SETNX 获取分布式锁，仅一个实例执行 Loader：
- 获锁成功 → 调 L3 → 写 L2 → 释放锁
- 获锁失败 → 等待最多 500ms（轮询 L2），等待超时后降级直调 L3

Builder 配置：`.hotspotProtection(true)`（默认 `false`，热点场景按需开启）

### 缓存雪崩（Cache Avalanche）

**问题**：大量 key 使用相同 L2 TTL，批量写入后（如重启预热）同时到期，L3 瞬间被打满。

**防护**：L2 TTL 加随机 Jitter，打散过期时间：

```
actualTtl = l2Ttl + random(0, l2Ttl × jitterRatio)
```

Builder 配置：`.l2TtlJitter(0.1)`（默认 0.1，即 0~10% 随机偏移）

---

## Builder 完整示例（含防护配置）

```java
TieredCache<Long, DagGraph> cache = TieredCache.<Long, DagGraph>builder()
    .name("canvas-config")
    .l1MaxSize(500)
    .l1RefreshAfterWrite(Duration.ofHours(2))
    .l2KeyPrefix("canvas:config:")
    .l2Ttl(Duration.ofHours(24))
    .l2TtlJitter(0.1)                        // 缓存雪崩防护
    .nullValueTtl(Duration.ofMinutes(5))      // 缓存穿透防护
    .hotspotProtection(true)                  // 缓存击穿防护（热点场景）
    .loader(id -> loadFromDb(id))
    .valueType(DagGraph.class)
    .build(manager);
```

---

## 不在范围内

- 多 L2 后端（Memcached 等）：Redis 是当前唯一 L2
- 缓存预热（Warmup）：由各业务自行决定
- Bloom Filter 防穿透（适用于海量随机 key 场景，当前 key 空间可控）
