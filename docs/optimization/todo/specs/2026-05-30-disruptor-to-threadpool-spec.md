# Spec: Disruptor → ThreadPoolExecutor/VirtualThread 替换

> **编号:** D | **严重度:** Medium | **迁移难度:** Easy

## Problem

LMAX Disruptor（65536 slot ring buffer, YieldingWaitStrategy, WorkerPool）用于营销触发任务分发。

**核心问题：**
1. Disruptor 为纳秒级超低延迟设计（金融交易），Canvas 引擎处理毫秒到秒级任务
2. `YieldingWaitStrategy` 自旋等事件，纯 CPU 浪费
3. Ring buffer 满时抛 `InsufficientCapacityException`，不如 `BlockingQueue` 阻塞等待
4. 事件对象复用（`event.reset()`）引入 stale data 泄漏风险

## Goal

替换为 `ThreadPoolExecutor` + bounded `LinkedBlockingQueue`，或 Java 21 虚拟线程 + `Semaphore` 限流。

## Scope

### In Scope
- `CanvasDisruptorService` 替换
- `publish()` → `executor.submit()`
- 背压机制（BlockingQueue 阻塞 vs Semaphore 限流）
- 事件对象不复用，消除 stale data 风险

### Out of Scope
- DAG 引擎重构（问题 A+B）
- 服务拆分（问题 C）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `CanvasDisruptorService.java` | Rewrite | Disruptor → ThreadPoolExecutor |
| `application.yml` | Modify | 线程池配置替代 disruptor 配置 |
| `pom.xml` | Modify | 移除 disruptor 依赖 |

## Success Criteria

1. 无 Disruptor 依赖
2. 背压通过 BlockingQueue 阻塞实现（非异常）
3. 无事件对象复用
4. 功能等价，所有测试通过
