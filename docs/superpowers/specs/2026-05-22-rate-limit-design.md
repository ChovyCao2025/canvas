# 下游调用速率控制设计（优化点 #12）

## 背景

`ApiCallHandler` 对下游接口无任何速率保护，高并发场景下（如大规模定时触发）会瞬间向下游发送海量请求，导致下游崩溃。

## 设计

### 速率限制策略：Fixed Window Counter（Redis INCR）

每个 `apiKey` 维护一个按秒滑动的计数器：

```
Key: canvas:ratelimit:{apiKey}:{epochSecond}
操作: INCR → 若 count==1 则 EXPIRE 2s（防 key 永不过期）
判断: count > rateLimitPerSec → 拒绝
```

**优点**：原子操作、低延迟、无 Lua 脚本依赖  
**缺点**：窗口边界最多放过 2× 限额（可接受，目标是下游保护而非精确计费）  
**行为**：超限时 fail-fast 返回 `NodeResult.fail(...)`，不阻塞

### 数据模型

`ApiDefinition` 加字段 `rate_limit_per_sec INT DEFAULT NULL`：
- `NULL` = 不限制（默认）
- `N` = 每秒最多 N 次

DDL（V38 migration）：

```sql
ALTER TABLE api_definition
    ADD COLUMN rate_limit_per_sec INT DEFAULT NULL
        COMMENT '每秒最大调用次数，NULL 表示不限制';
```

### ApiCallHandler 改动

在发起 HTTP 请求之前插入 Redis 检查：

```java
if (def.getRateLimitPerSec() != null) {
    String key = "canvas:ratelimit:" + apiKey + ":" + Instant.now().getEpochSecond();
    Long count = redis.opsForValue().increment(key);
    if (count == 1) redis.expire(key, Duration.ofSeconds(2));
    if (count != null && count > def.getRateLimitPerSec()) {
        return Mono.just(NodeResult.fail("API_CALL: 接口 " + apiKey + " 调用已达速率限制（" + def.getRateLimitPerSec() + " req/s）"));
    }
}
```

### 管理接口

`ApiDefinitionController.update()` 已接受完整 `ApiDefinition` 对象，实体加字段后自动支持通过 PUT 接口配置 `rateLimitPerSec`，无需新增控制器方法。

## 不在范围内

- 滑动窗口（Sorted Set 实现）——精度更高但内存更大，现阶段不必要
- 熔断/降级（Circuit Breaker）——留后续迭代
- 前端管理页新增速率限制输入框——留后续迭代（当前通过 API 工具配置）
