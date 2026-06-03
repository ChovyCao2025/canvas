# Spec: 单体服务拆分

> **编号:** C | **严重度:** High | **迁移难度:** Medium

## Problem

一个 `canvas-engine` 服务承担 29 个 API Controller、触发器解析、DAG 执行、Handler 分发、投递、人群计算、Wait 管理、版本管理、MQ 消费、调度、WebSocket 通知、DLQ 管理。

**核心问题：**
- 不同模块流量特征截然不同：API（低QPS低延迟）、MQ消费（高QPS突发）、定时触发（周期性批处理）、人群计算（CPU密集型可能数分钟）、投递（外部HTTP延迟不确定）
- 人群批量计算创建临时 DataSource 做全表扫描，与毫秒级实时事件触发跑在同一 JVM
- 一个慢的人群计算可饿死 `boundedElastic` 线程池，拖停所有 DAG 执行

## Goal

拆分为至少 3 个独立服务：
1. **Canvas API Service** — CRUD + 版本管理
2. **Canvas Engine Service** — DAG 执行 + 触发 + 投递
3. **Audience Compute Service** — 批量人群计算

## Scope

### In Scope
- 服务边界定义
- 共享 DB 访问解耦
- CanvasExecutionService 依赖图拆分
- 服务间通信（Feign/gRPC）
- 独立部署配置

### Out of Scope
- WebFlux → MVC 迁移（问题 A+B）
- 数据基建（问题 K/L/M/O）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `pom.xml` (parent) | Modify | 多模块结构 |
| `canvas-api/` | Create | API 服务模块 |
| `canvas-engine/` | Modify | 引擎服务模块（精简） |
| `canvas-audience/` | Create | 人群计算服务模块 |
| `AudienceBatchComputeService.java` | Move | 移至 canvas-audience |
| `AudienceUserResolver.java` | Move | 移至 canvas-audience |
| Feign clients | Create | 服务间调用接口 |

## Success Criteria

1. 人群计算服务可独立扩缩容
2. API 服务延迟不受人群计算影响
3. 引擎服务崩溃不影响 API CRUD
4. 各服务可独立部署
