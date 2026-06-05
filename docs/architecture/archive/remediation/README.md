# 架构问题整改方案 (2026-05-31)

> **背景**: 基于 `/architect` 命令的多轮架构审查，识别出35类问题、430+项具体缺陷。本文档为索引，详细实施方案见各分册。

---

## 问题总览（35类问题）

| # | 问题类别 | P0 | P1 | P2 | 总项数 | 工时 | 详细文档 |
|---|---------|----|----|-----|--------|------|---------|
| 1 | 单体巨石无边界 | - | - | - | 1 | 38h | [part1-structure](./part1-structure.md) |
| 2 | WebFlux+MyBatis矛盾 | - | - | - | 1 | 33h | [part1-structure](./part1-structure.md) |
| 3 | 单库单连接池耦合 | - | - | - | 1 | 30h | [part1-structure](./part1-structure.md) |
| 4 | Handler平铺 | - | - | - | 1 | 7h | [part1-structure](./part1-structure.md) |
| 5 | Service层缺失 | - | - | - | 1 | 40h | [part1-structure](./part1-structure.md) |
| 6 | DO直接暴露 | - | - | - | 1 | 40h | [part1-structure](./part1-structure.md) |
| 7 | 安全漏洞 | 3 | 6 | 1 | 10 | 9h | [part2-security-concurrency](./part2-security-concurrency.md) |
| 8 | 并发安全缺陷 | 3 | 3 | 2 | 8 | 16h | [part2-security-concurrency](./part2-security-concurrency.md) |
| 9 | 异常处理缺陷 | 0 | 3 | 5 | 8 | 4.5h | [part2-security-concurrency](./part2-security-concurrency.md) |
| 10 | 资源泄露 | 1 | 3 | 1 | 5 | 10h | [part2-security-concurrency](./part2-security-concurrency.md) |
| 11 | 无全局状态管理 | - | - | - | 3 | 24h | [part3-frontend](./part3-frontend.md) |
| 12 | API层缺陷 | - | - | - | 3 | 30h | [part3-frontend](./part3-frontend.md) |
| 13 | 路由权限缺陷 | - | - | - | 4 | 6h | [part3-frontend](./part3-frontend.md) |
| 14 | TypeScript any泛滥 | - | - | - | 4 | 14h | [part3-frontend](./part3-frontend.md) |
| 15 | 前端性能问题 | - | - | - | 3 | 9h | [part3-frontend](./part3-frontend.md) |
| 16 | 无生产部署方案 | - | - | - | 5 | 24h | [part4-ops](./part4-ops.md) |
| 17 | 配置管理缺陷 | - | - | - | 5 | 10h | [part4-ops](./part4-ops.md) |
| 18 | 可观测性缺失 | - | - | - | 4 | 20h | [part4-ops](./part4-ops.md) |
| 19 | MQ配置不完整 | - | - | - | 4 | 8h | [part4-ops](./part4-ops.md) |
| 20 | Flyway无回滚 | - | - | - | 1 | 8h | [part4-ops](./part4-ops.md) |
| 21 | DAG引擎核心设计 | 2 | 7 | 4 | 19 | 67h | [part5-engine-deep](./part5-engine-deep.md) |
| 22 | 数据模型与持久化 | 4 | 6 | 3 | 17 | 52h | [part5-engine-deep](./part5-engine-deep.md) |
| 23 | API层与集成设计 | 2 | 5 | 3 | 12 | 44h | [part5-engine-deep](./part5-engine-deep.md) |
| 24 | 前端架构深度 | 2 | 5 | 4 | 13 | 60.5h | [part5-engine-deep](./part5-engine-deep.md) |
| 25 | 业务逻辑正确性 | 4 | 10 | 4 | 34 | 44h | [part6-logic-testing](./part6-logic-testing.md) |
| 26 | 测试架构与覆盖 | 2 | 4 | 4 | 28 | 84h | [part6-logic-testing](./part6-logic-testing.md) |
| 27 | 技术债与代码质量 | 0 | 2 | 4 | 30 | 22h | [part6-logic-testing](./part6-logic-testing.md) |
| 28 | 可观测性与韧性 | 3 | 5 | 4 | 28 | 40h | [part7-resilience](./part7-resilience.md) |
| 28.7 | Reactor线程模型违规 | 5 | 9 | 66 | 80+ | 17h | [part7-resilience](./part7-resilience.md) |
| 28.8 | API输入验证零覆盖 | 0 | 1 | 0 | 29 Controller | 27h | [part7-resilience](./part7-resilience.md) |
| 28.9 | 硬删除不一致 | 0 | 0 | 1 | 13 | 11h | [part7-resilience](./part7-resilience.md) |
| 28.10 | @Transactional+Reactive陷阱 | 0 | 1 | 0 | 17 | 13h | [part7-resilience](./part7-resilience.md) |
| 28.11 | 前端状态与数据流 | 0 | 1 | 3 | 62 any+91 useEffect | 21h | [part7-resilience](./part7-resilience.md) |
| **合计** | | **34** | **80** | **124** | **~430项** | **~825.5h** | |

---

## 优先级排序（Top 15 紧急修复）

1. **S1/S2/S3** — 凭据硬编码 + JWT空密钥 → 立即修复
2. **S5** — CORS 凭据窃取风险 → 立即修复
3. **23.5** — 开放执行端点无认证 → 任何网络可达者可执行画布并冒充用户
4. **25.1** — 画布状态机无守卫 → KILLED画布可被静默重新发布
5. **25.2** — 频率控制非原子 → 进程崩溃永久占用用户频率位
6. **28.7.A** — 5处.block()未包裹boundedElastic → 阻塞Netty事件循环
7. **C1/C2/C3** — CircuitBreaker 竞态 → 可能导致熔断失效
8. **R1** — DagEngine 火忘 subscribe → 可能导致超时回调误触发
9. **28.7.B** — 4处.subscribe()无调度器 → DAG执行/旅程触发在调用者线程
10. **28.8** — 29个Controller零@Valid → 170+端点无输入验证
11. **28.10** — 17处@Transactional在Reactive上下文 → 事务不生效+DB/Redis不一致
12. **22.3** — tenant_id nullable且ORM不可见 → 多租户形同虚设
13. **25.3** — 版本清理可nullify运行中graphJson → 恢复执行崩溃
14. **28.7.C** — 3处Thread.sleep() → 阻塞Reactor线程
15. **28.9** — 13处硬删除 → 审计追踪断裂

---

## 实施路线图

```
Week 1: 安全与并发修复（最紧急）
    ├── P0安全漏洞修复 (9h)
    ├── CircuitBreaker/ExecutionContext并发修复 (16h)
    └── 资源泄露修复 (10h)

Week 2-3: 架构结构性整改
    ├── Phase 1 模块化拆分 (38h)
    ├── Phase 2 解决Reactor矛盾 (33h)
    └── Phase 3 多数据源隔离 (30h)

Week 4: 引擎核心 + 数据模型 + 业务逻辑
    ├── DagEngine God Class拆分 (67h)
    ├── 数据模型富领域+Outbox (52h)
    ├── 画布状态机+幂等性+审计 (44h)
    ├── Handler分组 (7h)
    └── 异常处理+MQ修复 (12.5h)

Week 5: API层 + 前端架构 + 测试
    ├── API版本化+DTO+WebClient统一 (44h)
    ├── 画布编辑器拆分+节点注册表 (60.5h)
    ├── 测试基础设施+关键路径测试 (84h)
    └── 全局状态管理+路由+TypeScript (44h)

Week 6: 韧性 + 运维 + 技术债
    ├── 可观测性+优雅关闭+熔断 (40h)
    ├── Reactor线程模型修复 (17h)
    ├── API验证+硬删除+事务修复 (51h)
    └── 技术债清理 (22h)

Week 7-8: 测试补全 + 前端深度
    ├── Handler测试覆盖补全 (20h)
    ├── 前端组件测试 (12h)
    ├── E2E测试 (20h)
    └── 前端状态数据流修复 (21h)
```

**总工时**: ~825.5h（约103人日，8周并行 / 16周串行）

---

## 分册文件

| 文件 | 内容 |
|---------|------|
| [part1-structure.md](./part1-structure.md) | 问题1-6：架构结构性问题 |
| [part2-security-concurrency.md](./part2-security-concurrency.md) | 问题7-10：安全/并发/异常/资源泄露 |
| [part3-frontend.md](./part3-frontend.md) | 问题11-15：前端架构问题 |
| [part4-ops.md](./part4-ops.md) | 问题16-20：运维与基础设施 |
| [part5-engine-deep.md](./part5-engine-deep.md) | 问题21-24：引擎核心/数据模型/API集成/前端深度 |
| [part6-logic-testing.md](./part6-logic-testing.md) | 问题25-27：业务逻辑/测试/技术债 |
| [part7-resilience.md](./part7-resilience.md) | 问题28/28.7-28.11：韧性/线程模型/验证/删除/事务/前端状态 |

**总工时**: ~825.5h（约103人日，8周并行 / 16周串行）
