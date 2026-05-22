# 多级缓存 SDK 设计（优化点 #9）

## 背景

`CanvasConfigCache` 实现了 L1(Caffeine) + L2(Redis) + L3(Loader) + Pub/Sub 失效广播的三级缓存模式，但强耦合于业务。本 SDK 将该模式**泛型化、零业务依赖地抽离为独立 Maven 模块**，任意服务 3 行 Builder 即可接入。

---

## Maven 模块结构

```
backend/
├── pom.xml                          ← 聚合父 pom
├── canvas-cache-sdk/                ← SDK 模块（仅依赖 Redis/Caffeine/Jackson）
│   └── src/main/java/org/chovy/cache/
│       ├── TieredCache.java              ← 核心接口（同步）
│       ├── ReactiveTieredCache.java      ← 响应式视图接口
│       ├── TieredCacheImpl.java          ← 实现（同步 + 异步共享 L1/L2）
│       ├── TieredCacheBuilder.java       ← Fluent Builder
│       ├── TieredCacheManager.java       ← Spring Bean，管理 Pub/Sub + 实例注册
│       ├── strategy/
│       │   ├── LoaderFailureStrategy.java
│       │   ├── RedisFailureStrategy.java
│       │   ├── ConsistencyStrategy.java
│       │   └── DeserializeFailureStrategy.java
│       ├── testing/
│       │   ├── InMemoryTieredCache.java  ← 测试用内存实现
│       │   └── TestTieredCacheConfig.java ← @TestTieredCache 自动装配
│       └── autoconfigure/
│           └── TieredCacheAutoConfiguration.java
└── canvas-engine/                   ← 添加 SDK 依赖，迁移 CanvasConfigCache
```

---

## 核心接口设计

### TieredCache\<K, V\>（同步）

```java
public interface TieredCache<K, V> {
    String name();

    /** L1 → L2 → Loader 三级查找，自动回填上级 */
    Optional<V> get(K key);

    /** 写 L2 → 写 L1（L2 失败则 L1 不写，保证 L1 ⊆ L2） */
    void put(K key, V value);

    /** 清除 L1 + L2，Pub/Sub 广播所有实例 */
    void invalidate(K key);

    /**
     * 双删一致性写：删 L2 → 执行 writeAction（DB 写）→ sleep(delayMs) → 再删 L2。
     * 收缩竞态窗口，适合对一致性要求较高的写操作。
     */
    void safeWrite(K key, Runnable writeAction, long delayMs);

    /** 强制从 Loader 重新加载，更新 L1 + L2 */
    void refresh(K key);

    /** 切换到响应式视图，共享同一 L1/L2 实例 */
    ReactiveTieredCache<K, V> asReactive();
}
```

### ReactiveTieredCache\<K, V\>（响应式视图）

```java
public interface ReactiveTieredCache<K, V> {
    /** L1（同步查）→ L2（响应式 Redis）→ Loader（响应式包装） */
    Mono<Optional<V>> get(K key);
    Mono<Void>        put(K key, V value);
    Mono<Void>        invalidate(K key);
    Mono<Void>        refresh(K key);
}
```

**内部实现**：L1 始终同步（Caffeine 不支持响应式），L2 走 `ReactiveStringRedisTemplate`，Loader 用 `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` 包装。

---

## 策略枚举

### LoaderFailureStrategy — L3 加载失败

```java
public enum LoaderFailureStrategy {
    THROW,          // 默认：直接抛出，调用方处理
    RETURN_STALE,   // L1 有旧值则返回旧值，否则抛出（refreshAfterWrite 失败时保留旧值）
    RETURN_EMPTY    // 静默返回 Optional.empty()（慎用，会掩盖问题）
}
```

### RedisFailureStrategy — Redis 故障

```java
public enum RedisFailureStrategy {
    FALLTHROUGH,   // 默认：Redis 异常时降级穿透 L3（读）/ 静默忽略（写）
    FAIL_FAST      // Redis 异常直接抛出，交上层熔断器处理
}
```

读路径和写路径**分别配置**：
```java
.onRedisReadFailure(FALLTHROUGH)   // 读时 Redis 挂了 → 穿 L3
.onRedisWriteFailure(FALLTHROUGH)  // 写时 Redis 挂了 → 只写 L1，打 warn 日志
```

### DeserializeFailureStrategy — L2 反序列化失败

```java
public enum DeserializeFailureStrategy {
    FALLTHROUGH_TO_L3,  // 默认：删坏 key + 穿透 L3（自愈）
    THROW               // 明确报错，适合排查期
}
```

---

## 三大防护

### 1. 缓存穿透（Null Sentinel）

Loader 返回 `null` 时，向 L2 写入哨兵值 `__NULL__`，TTL 使用 `nullValueTtl`（默认 5 分钟，远短于正常 TTL）。后续请求命中哨兵直接返回 `Optional.empty()`，不再调 Loader。

```java
.nullValueTtl(Duration.ofMinutes(5))   // 默认开启
```

### 2. 缓存击穿（Distributed Lock）

L2 miss 后，通过 Redis `SETNX` 获取分布式锁再调 L3：
- 获锁成功 → 调 L3 → 写 L2 → 释放锁
- 获锁失败 → 轮询等待 L2 填充（最多 500ms）→ 超时后降级直调 L3
- 锁 TTL = 30s（防止 Loader 超时导致死锁）

Caffeine `LoadingCache` 已保证**单实例内**同一 key 只有一个线程执行 load；本锁只用于**跨实例**场景。

```java
.hotspotProtection(true)   // 默认 false，热点场景开启
```

### 3. 缓存雪崩（TTL Jitter）

L2 写入时对 TTL 加随机偏移，打散批量过期：

```
actualTtl = l2Ttl × (1 + random(0, jitterRatio))
```

```java
.l2TtlJitter(0.1)   // 默认 0.1，即 TTL 在 [100%, 110%] 范围随机分布
```

---

## Schema 演进

L2 key 带 schema 版本号，schema 变更时升版本绕过旧数据：

```java
.keySchemaVersion(2)                              // L2 key 格式：{prefix}v2:{key}
.onDeserializeFailure(FALLTHROUGH_TO_L3)          // 旧格式数据反序列化失败自愈
```

---

## L1 续期机制

使用 Caffeine `refreshAfterWrite`（非 `expireAfterWrite`）：

- 到达 refresh 时间后，**下次 get 时异步刷新**，当前请求返回旧值（stale-while-revalidate）
- 刷新逻辑：**先查 L2**，命中则续期 L1（不穿透 L3）；L2 也过期才调 Loader
- 刷新线程：**Java 21 虚拟线程**（`Executors.newVirtualThreadPerTaskExecutor()`），不占 carrier thread，阻塞 I/O 无感

`refreshAfterWrite` TTL 同时是 **Pub/Sub 失效事件丢失时的最大陈旧窗口**（即可靠性兜底）。

---

## Pub/Sub 失效广播

```
调用 invalidate(key)
  → L1 本实例失效
  → Redis DEL L2 key
  → PUBLISH tiered-cache:{name}:invalidate "{schemaVersion}:{key}"
       ↓ 所有存活实例订阅（PSUBSCRIBE tiered-cache:*:invalidate）
  → 各实例清自己 L1
```

**可靠性保证**：Pub/Sub 是 fire-and-forget，消息可能丢失。丢失后果：该实例 L1 保留旧值，直到 `refreshAfterWrite` 触发（最大陈旧 = L1 refresh TTL）。L2 已被 `DEL` 清除，刷新时必然穿透到新数据，**不会永久陈旧**。

---

## 可观测性（Micrometer 自动埋点）

| 指标名 | 类型 | 说明 |
|--------|------|------|
| `tiered_cache_hits_total` | Counter | tag: `cache`, `level`(L1/L2/L3) |
| `tiered_cache_misses_total` | Counter | tag: `cache`, `level` |
| `tiered_cache_load_duration_seconds` | Timer | L3 加载耗时，tag: `cache` |
| `tiered_cache_penetration_total` | Counter | 空值哨兵命中次数（穿透防护触发） |
| `tiered_cache_hotspot_lock_wait_total` | Counter | 分布式锁等待次数（击穿防护触发） |
| `tiered_cache_pubsub_published_total` | Counter | 失效广播发送次数 |
| `tiered_cache_loader_failure_total` | Counter | L3 加载失败次数，tag: `strategy` |

```java
.enableMetrics(true)           // 默认 true
.meterRegistry(meterRegistry)  // 不传则自动从 Spring context 获取
```

---

## 测试支持

### InMemoryTieredCache

实现 `TieredCache<K,V>` 接口，L2 用 `ConcurrentHashMap` 替代 Redis，无 Pub/Sub，无网络依赖：

```java
// 测试中直接使用
TieredCache<Long, DagGraph> cache = InMemoryTieredCache.of(id -> loadFromDb(id));
```

### @TestTieredCache

Spring test scope 下自动将所有 `TieredCache` Bean 替换为内存实现，零改动：

```java
@SpringBootTest
@TestTieredCache   // 所有 TieredCache Bean 自动切换为 InMemoryTieredCache
class MyServiceTest { ... }
```

---

## Builder 完整示例

```java
TieredCache<Long, DagGraph> cache = TieredCache.<Long, DagGraph>builder()
    .name("canvas-config")
    .l1MaxSize(500)
    .l1RefreshAfterWrite(Duration.ofHours(2))
    .l2KeyPrefix("canvas:config:")
    .l2Ttl(Duration.ofHours(24))
    .l2TtlJitter(0.1)                              // 雪崩防护
    .keySchemaVersion(1)                           // Schema 演进
    .nullValueTtl(Duration.ofMinutes(5))           // 穿透防护
    .hotspotProtection(false)                      // 击穿防护（热点时开）
    .onLoaderFailure(LoaderFailureStrategy.THROW)
    .onRedisReadFailure(RedisFailureStrategy.FALLTHROUGH)
    .onRedisWriteFailure(RedisFailureStrategy.FALLTHROUGH)
    .onDeserializeFailure(DeserializeFailureStrategy.FALLTHROUGH_TO_L3)
    .enableMetrics(true)
    .loader(versionId -> {
        CanvasVersion v = mapper.selectById(versionId);
        return parser.parse(v.getGraphJson());
    })
    .valueType(DagGraph.class)
    .build(cacheManager);
```

---

## 迁移 CanvasConfigCache

迁移后约 30 行，业务逻辑全委托 SDK：

```java
@Service
public class CanvasConfigCache {
    private final TieredCache<Long, DagGraph> delegate;

    public DagGraph get(Long canvasId, Long versionId) {
        return delegate.getOrThrow(versionId);
    }

    public void invalidate(Long canvasId, Long versionId) {
        delegate.invalidate(versionId);
    }
}
```

---

## 不在范围内

- Bloom Filter 防穿透（key 空间可控，Null Sentinel 已够用）
- 多 L2 后端（Memcached 等）
- 缓存预热（Warmup）
- SDK 发布到私仓（当前 monorepo 内使用，稳定后可提取）
