# 定时任务随机 Jitter 防雪崩设计（优化点 #10）

## 背景

`CanvasSchedulerService.triggerForAllUsers()` 在 CRON 触发时对所有用户立即并发触发，大规模离线人群（万级用户）会在同一秒内产生海量请求，引发系统雪崩。

## 根因

```java
for (String userId : userIds) {
    executionService.trigger(...).subscribe();  // 无延迟，全部同时触发
}
```

## 解决方案

为每个用户生成 `[0, maxJitterMs)` 区间内的随机延迟，使用 `Mono.delay()` 实现非阻塞错峰。

```java
long maxJitterMs = ...; // 可配置，默认 300_000ms（5分钟）
for (String userId : userIds) {
    long delayMs = ThreadLocalRandom.current().nextLong(0, maxJitterMs);
    Mono.delay(Duration.ofMillis(delayMs))
        .then(Mono.fromRunnable(() ->
            executionService.trigger(canvasId, userId, ...)
                .subscribe(null, e -> log.warn(...))
        ))
        .subscribe();
}
```

### 配置参数

通过 `application.properties` 可调：

```properties
# 定时任务触发的最大随机抖动时间（毫秒），0 表示不抖动
canvas.scheduler.jitter-max-ms=300000
```

`jitterMaxMs = 0` 时退化为原始行为（方便单测验证）。

## 不在范围内

- MQ 削峰（优化点 #5，独立设计）
- 触发限流（`TriggerPreCheckService` 已有 perUserDailyLimit 等限制）
- 用户分批并发度控制（可作后续迭代）
