# 下游速率控制 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为每个 ApiDefinition 配置每秒最大调用次数，ApiCallHandler 使用 Redis Fixed Window Counter 拦截超限请求，保护下游接口不被瞬间流量打垮。

**Architecture:** Flyway V38 加列 → 实体加字段 → ApiCallHandler 注入 Redis 并在 HTTP 调用前检查 → 现有 PUT 接口自动支持配置。

**Tech Stack:** Java 17, Spring WebFlux, Redis (StringRedisTemplate), MyBatis-Plus

---

## File Map

| Action | File |
|--------|------|
| Create | `src/main/resources/db/migration/V38__api_rate_limit.sql` |
| Modify | `domain/meta/ApiDefinition.java` |
| Modify | `engine/handlers/ApiCallHandler.java` |
| Create | `src/test/java/.../engine/handlers/ApiCallHandlerRateLimitTest.java` |

路径前缀：`backend/canvas-engine/src/main/java/org/chovy/canvas/`

---

## Task 1：Flyway migration + 实体加字段

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V38__api_rate_limit.sql`
- Modify: `domain/meta/ApiDefinition.java`

- [ ] **Step 1：创建 V38 migration**

```sql
-- V38: 为 api_definition 增加速率限制配置
ALTER TABLE api_definition
    ADD COLUMN rate_limit_per_sec INT DEFAULT NULL
        COMMENT '每秒最大调用次数，NULL 表示不限制';
```

- [ ] **Step 2：在 ApiDefinition 实体加字段**

在 `description` 字段之后添加：

```java
/** 每秒最大调用次数，null 表示不限制（对应 TriggerPreCheckService 外的速率保护） */
private Integer rateLimitPerSec;
```

- [ ] **Step 3：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4：Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V38__api_rate_limit.sql \
        backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/ApiDefinition.java
git commit -m "feat: add rate_limit_per_sec field to api_definition"
```

---

## Task 2：ApiCallHandler 注入 Redis + 实现速率检查（TDD）

**Files:**
- Create: `src/test/java/org/chovy/canvas/engine/handlers/ApiCallHandlerRateLimitTest.java`
- Modify: `engine/handlers/ApiCallHandler.java`

- [ ] **Step 1：写失败测试**

创建 `ApiCallHandlerRateLimitTest.java`：

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.domain.meta.ApiDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiCallHandlerRateLimitTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("未超限时返回 false（允许通过）")
    void under_limit_returns_false() {
        when(valueOps.increment(anyString())).thenReturn(1L); // 第一次请求

        boolean exceeded = ApiCallHandler.isRateLimitExceeded(redis, "test_api", 10, Instant.now());

        assertThat(exceeded).isFalse();
    }

    @Test
    @DisplayName("超限时返回 true（拒绝）")
    void over_limit_returns_true() {
        when(valueOps.increment(anyString())).thenReturn(11L); // 第 11 次，超过 10/s

        boolean exceeded = ApiCallHandler.isRateLimitExceeded(redis, "test_api", 10, Instant.now());

        assertThat(exceeded).isTrue();
    }

    @Test
    @DisplayName("第一次请求时设置 TTL")
    void first_request_sets_expire() {
        when(valueOps.increment(anyString())).thenReturn(1L);

        ApiCallHandler.isRateLimitExceeded(redis, "test_api", 10, Instant.now());

        verify(redis).expire(anyString(), eq(java.time.Duration.ofSeconds(2)));
    }

    @Test
    @DisplayName("非第一次请求不重置 TTL")
    void subsequent_request_does_not_reset_expire() {
        when(valueOps.increment(anyString())).thenReturn(5L);

        ApiCallHandler.isRateLimitExceeded(redis, "test_api", 10, Instant.now());

        verify(redis, never()).expire(anyString(), any());
    }
}
```

- [ ] **Step 2：运行测试确认失败**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=ApiCallHandlerRateLimitTest -q
```

Expected: FAIL — `isRateLimitExceeded` 不存在

- [ ] **Step 3：在 ApiCallHandler 注入 Redis 并添加 isRateLimitExceeded 静态方法**

在 `ApiCallHandler` 类中添加字段注入：

```java
private final StringRedisTemplate redis;
```

在类末尾添加静态方法（便于单测）：

```java
/**
 * Fixed Window Counter 速率检查。
 * @return true = 超限（应拒绝），false = 未超限（允许通过）
 */
static boolean isRateLimitExceeded(StringRedisTemplate redis,
                                    String apiKey, int limitPerSec, Instant now) {
    String key = "canvas:ratelimit:" + apiKey + ":" + now.getEpochSecond();
    Long count = redis.opsForValue().increment(key);
    if (count != null && count == 1) {
        redis.expire(key, java.time.Duration.ofSeconds(2));
    }
    return count != null && count > limitPerSec;
}
```

- [ ] **Step 4：在 executeAsync() 中调用速率检查**

在 `def == null` 检查之后、构建请求体之前，插入：

```java
// 速率限制检查
if (def.getRateLimitPerSec() != null) {
    if (isRateLimitExceeded(redis, apiKey, def.getRateLimitPerSec(), Instant.now())) {
        log.warn("[API_CALL] 速率限制 apiKey={} limit={}/s", apiKey, def.getRateLimitPerSec());
        return Mono.just(NodeResult.fail(
                "API_CALL: 接口 " + apiKey + " 调用已达速率限制（" + def.getRateLimitPerSec() + " req/s）"));
    }
}
```

添加 import：`import java.time.Instant;`（若未引入）

- [ ] **Step 5：运行测试确认通过**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=ApiCallHandlerRateLimitTest -q
```

Expected: `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 6：运行全量测试**

```bash
cd backend && mvn test -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ApiCallHandler.java \
        backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ApiCallHandlerRateLimitTest.java
git commit -m "feat: add Redis fixed-window rate limiting to ApiCallHandler"
```
