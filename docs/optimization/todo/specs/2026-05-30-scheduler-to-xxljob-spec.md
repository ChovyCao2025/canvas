# Spec: Spring Scheduler → XXL-Job 分布式调度

> **编号:** N | **严重度:** Critical | **迁移难度:** Medium
> **代码自认不可生产：** LocalTaskScheduleRegistrar.java 注释明确写了 "not a production-grade distributed scheduler"

## Problem

当前定时任务基于 Spring `ThreadPoolTaskScheduler`（线程池大小 4）+ `LocalTaskScheduleRegistrar`（JVM 本地 ConcurrentHashMap 存调度句柄）。

**核心问题：**
1. **调度状态丢失** — JVM 重启后所有 ScheduledFuture 消失，cron 任务不会自动恢复
2. **多实例重复触发** — 同一 cron 触发器在每个实例上注册，触发 N 次
3. **线程池只有 4** — 10 个画布同时到 cron 时间点，6 个排队
4. **无分片/故障转移** — 实例宕机后调度任务无人接管
5. **PendingJitterGroup** 也是 JVM 本地 ConcurrentHashMap，无法跨实例去重

## Goal

用 XXL-Job v3.4 替换 Spring Scheduler，实现生产级分布式调度。

## Target Architecture

### XXL-Job 集成
- 部署 XXL-Job Admin（依赖 MySQL，项目已有）
- 实现 `ScheduleRegistrar` 接口的 XXL-Job adapter
- 替换 `LocalTaskScheduleRegistrar`
- 调度状态持久化到 XXL-Job MySQL
- 多实例部署时同一 cron 只触发一次
- 故障转移和分片能力
- Admin UI 开箱即用

### 配置项
- XXL-Job Admin 地址
- 执行器AppName、端口
- AccessToken
- 日志保留天数

## Scope

### In Scope
- XXL-Job Admin 部署配置（Docker compose）
- `XxlJobScheduleRegistrar` 实现
- 替换 `@ConditionalOnMissingBean` 条件
- CanvasSchedulerService 适配
- PendingJitterGroup 外部化（Redis）
- 迁移现有 cron 触发器注册逻辑

### Out of Scope
- DAG 执行引擎重构（问题 A+B）
- 人群计算拆分（问题 C）
- 实时计算引擎（问题 M）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `LocalTaskScheduleRegistrar.java` | Keep | 保留作为本地开发 fallback |
| `XxlJobScheduleRegistrar.java` | Create | XXL-Job adapter 实现 ScheduleRegistrar |
| `SchedulerConfig.java` | Modify | 增加 XXL-Job 配置 |
| `CanvasSchedulerService.java` | Modify | 适配新调度注册器 |
| `application.yml` | Modify | XXL-Job 配置项 |
| `pom.xml` | Modify | 添加 xxl-job-core 依赖 |
| `docker-compose.yml` | Modify | 添加 XXL-Job Admin 服务 |
| `PendingJitterGroup` 相关 | Modify | JVM ConcurrentHashMap → Redis |

## Dependencies

- XXL-Job v3.4.0
- MySQL（已有，XXL-Job Admin 自带 schema）
- 项目现有 MySQL 实例可复用

## Risk Assessment

- **中风险：** 调度注册器替换影响所有定时触发画布
- **缓解策略：**
  1. `@ConditionalOnMissingBean` 已设计好，本地开发仍用 LocalTaskScheduleRegistrar
  2. 生产环境注入 XxlJobScheduleRegistrar
  3. 灰度：先迁移非关键画布的定时触发

## Success Criteria

1. 多实例部署时同一 cron 触发器只触发一次
2. JVM 重启后 cron 任务自动恢复
3. Admin UI 可查看/管理所有调度任务
4. 故障转移：一个实例宕机，其他实例接管
5. `LocalTaskScheduleRegistrar` 注释中承认的问题全部解决
