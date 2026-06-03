# DIST-PERF: 多机压力测试设计

## 1. 背景

当前 `docs/stressTest` 和 `tools/perf` 已经能支持比较完整的本地单机容量压测：fixture、smoke、accuracy、threshold、soak、report、cleanup 都有统一入口，并且容量报告必须经过 runner、verifier、trace 和副作用证据校验。

多机场景还不能只靠文档解决。现有 `perf-runner.mjs` 只按 `1..count` 生成请求序号，多台压测机同时使用同一个 `perfRunId` 会产生重复 `perfInputId` 和幂等键；如果每台机器使用不同 `perfRunId`，verifier 又无法一次性校验全局账本。现有 `report` 也只读取单个 `runner-summary.json`，不能计算全局成功数、失败数、持续时间和延迟分位。

因此，多机压测需要补齐分布式发压、分片、防重复、证据汇总和统一闸门。文档需要覆盖两种拓扑：多台压测机打同一后端入口，以及多台压测机打负载均衡后的多后端应用节点。

## 2. 目标

- 支持多台压测机共同使用一个 `perfRunId` 发压，并保证每个请求的 `perfInputId`、`seq`、direct 幂等键全局唯一。
- 支持压测入口是单个后端 URL，也支持压测入口是负载均衡 URL，LB 后面可以挂多台 `canvas-engine` 节点。
- 保留现有单机流程和命令，不破坏 `local-capacity-runbook.md`。
- 提供控制机生成计划、压测机执行 worker、控制机汇总报告的标准流程。
- 汇总报告必须经过全局 verifier；容量数字只能来自全局证据，不允许人工相加后发布。
- accuracy 模式在多机下继续校验账本、节点 trace、分支次数和 WireMock 副作用，防止高并发下核心画布引擎漏执行、重执行、错分支或重复投递。
- 中文文档要足够明确，让非专业用户能按角色执行：控制机、压测机、后端节点、依赖服务。

## 3. 非目标

- 不引入 Kubernetes、Terraform 或云厂商自动化部署。多后端节点的创建方式由执行环境决定，runbook 只定义拓扑和校验要求。
- 不实现跨机器远程 SSH 编排。第一版采用控制机生成计划、人工分发计划、各 worker 本地执行、再收集证据的模式。
- 不用平均值估算全局 p95/p99。没有足够延迟证据时，报告必须拒绝输出全局分位数。
- 不把多机压测结果自动等同于生产容量。生产容量仍需结合真实资源、DB/Redis/MQ/下游限额和安全系数外推。

## 4. 推荐拓扑

### 4.1 多压测机打单后端入口

```text
load-worker-01 \
load-worker-02  -> http://backend-or-lb:8080 -> canvas-engine
load-worker-03 /

canvas-engine -> MySQL
canvas-engine -> Redis
canvas-engine -> RocketMQ
canvas-engine -> WireMock
```

这个拓扑用于判断单台压测机是否已经成为瓶颈。后端可以还是单节点，压测入口可以是单 backend URL 或 LB URL。

### 4.2 多压测机打多后端节点

```text
load-worker-01 \
load-worker-02  -> http://lb:8080 -> canvas-engine-01 \
load-worker-03 /                    canvas-engine-02  -> MySQL / Redis / RocketMQ / WireMock
                                      canvas-engine-03 /
```

这个拓扑用于验证应用横向扩容。所有后端节点必须连接同一组 MySQL、Redis、RocketMQ 和 WireMock；否则 verifier 无法得到完整账本，副作用证据也不可比。

## 5. 命令设计

新增三个向导子命令：

- `distributed-plan`
  - 在控制机执行。
  - 输入总请求数、总并发、worker 数、worker ID 列表、base URL、mode、event code 或 canvas id。
  - 输出 `plan.json`，包含统一 `perfRunId`、worker 分片、每台机器的 count/concurrency、计划开始时间、证据目录结构和签名环境变量名。

- `distributed-worker`
  - 在每台压测机执行。
  - 读取 `plan.json` 和本机 `workerId`。
  - 根据计划调用底层 `perf-runner.mjs`，只发送本 worker 负责的全局序号。
  - 输出 `runner-summary.<workerId>.json` 和必要的本机 metadata。

- `distributed-report`
  - 在控制机执行。
  - 读取所有 worker summary，校验 worker 数量、worker ID、计划一致性、全局序号范围、请求数、失败数和时间窗口。
  - 汇总 runner 证据后调用全局 verifier。
  - accuracy 场景额外调用 side-effect verifier 和 trace expectation。
  - 输出 `distributed-summary.json`、`verifier.json`、`side-effect-verifier.json` 和最终 status。

保留已有 `fixture`、`smoke`、`accuracy`、`threshold`、`soak`、`report`、`cleanup`。多机流程不替代单机流程，而是在单机 accuracy 通过后执行。

## 6. 分片规则

每个 worker 使用同一个 `perfRunId`，但请求序号必须全局唯一。计划生成时为每台 worker 分配连续区间：

```text
worker-01: seq 1..100000
worker-02: seq 100001..200000
worker-03: seq 200001..300000
```

底层 runner 需要新增序号参数：

- `--seq-start <n>`：本 worker 第一个全局序号。
- `--seq-count <n>`：本 worker 发送请求数；和现有 `--count` 二选一或由向导统一传入。

`perfInputId` 继续使用现有格式：

```text
<perfRunId>:event:<globalSeq>
<perfRunId>:direct:<globalSeq>
<perfRunId>:audience:<globalSeq>
```

direct 的 `idempotencyKey` 同样使用 `globalSeq`。这样 verifier 可以按一个 `perfRunId` 汇总账本，副作用 verifier 也能按 `perfInputId + branch` 查重。

## 7. 延迟汇总

多机报告不能用各 worker 的 p95 平均值。底层 runner 需要额外输出延迟证据：

- `latencyBuckets`：固定边界的毫秒桶，便于合并计算近似 p95/p99。
- `minMs`、`maxMs`、`avgMs`：辅助诊断，不作为容量闸门的唯一依据。
- `p95Ms` 保留为单 worker 指标。

`distributed-report` 使用所有 worker 的 `latencyBuckets` 计算全局 p95 和 p99。如果任一 worker 缺少延迟桶，容量报告必须拒绝输出全局分位数，只能输出失败原因。

## 8. 证据目录

控制机生成：

```text
tmp/perf-distributed/<perfRunId>/
  plan.json
  workers/
    worker-01/runner-summary.json
    worker-02/runner-summary.json
    worker-03/runner-summary.json
  distributed-summary.json
  verifier.json
  side-effect-verifier.json
  monitor/
  report.json
```

每个 worker summary 必须包含：

- `perfRunId`
- `workerId`
- `seqStart`
- `seqCount`
- `sent`
- `success`
- `failed`
- `durationMs`
- `latencyBuckets`
- `settings`
- `machine`

`distributed-summary.json` 必须包含：

- 计划 worker 数和实际 worker 数。
- 全局 sent/success/failed。
- 全局 duration，使用最早 startedAt 到最晚 finishedAt 计算。
- 全局 p95/p99。
- 所有 worker 的机器信息和单 worker 结果。
- verifier verdict 和 accuracy side-effect verdict。

## 9. 多后端节点要求

多 backend 节点压测时，文档必须要求执行人确认：

- 所有 backend 节点运行同一个镜像或同一个 git commit。
- 所有 backend 节点使用相同 `CANVAS_EVENT_REPORT_SECRET`。
- 所有 backend 节点连接同一个 MySQL、Redis、RocketMQ 和 WireMock。
- LB 健康检查能识别后端不可用节点。
- LB 分发策略和连接复用策略记录到报告。
- 每个 backend 节点的 CPU、内存、GC、线程池、Hikari、Disruptor 指标单独采集。
- MySQL、Redis、RocketMQ 和 WireMock 作为共享瓶颈单独采集。

如果任一 backend 节点缺监控证据，多机容量报告只能标记为证据不完整，不能发布容量结论。

## 10. 执行流程

多机完整流程：

```text
单机 doctor
  -> 单机 fixture
  -> 单机 smoke
  -> 单机 accuracy
  -> distributed-plan
  -> 分发 plan.json 到每台 worker
  -> 每台 worker 执行 distributed-worker
  -> 收集 workers/*/runner-summary.json
  -> distributed-report
  -> 多机 threshold 或 soak 结论
  -> cleanup
```

多机 threshold 第一版不做自动跨机器阶梯编排。runbook 采用多轮 `distributed-plan`：每个阶段一个新的 `perfRunId`，逐档增加总并发。某一档 `distributed-report` 失败时停止，上一档为稳定档。

多机 soak 使用 threshold 的最后稳定档作为输入，默认至少 30 分钟。`distributed-report` 必须检查全局持续时间，不允许用单 worker duration 替代。

## 11. 停止条件

出现任一情况立即停止：

- 任一 worker summary 缺失或 `perfRunId` 不一致。
- 任一 worker 报告 `failed > 0`。
- worker 序号区间重叠或有缺口。
- 全局 verifier 不是 `PASS`。
- accuracy 的 side-effect verifier 不是 `PASS`。
- trace verifier 出现 mismatch、failed trace、duplicate success 或 pending buffer。
- 任一 backend 节点重启、OOM kill 或健康检查失败。
- DB、Redis、RocketMQ 或 WireMock 持续报错。
- 监控缺失，或压测结束后才补采关键监控。

停止后只能整理失败报告和排障信息，不能发布容量数字。

## 12. 实施范围

代码改动：

- `tools/perf/perf-runner.mjs`
  - 增加 `--seq-start`，让多 worker 能生成全局唯一序号。
  - summary 增加 `workerId`、`seqStart`、`seqCount` 和 `latencyBuckets`。

- `tools/perf/distributed-report.mjs`
  - 新增分布式计划、worker summary 汇总、全局分位计算和证据校验。
  - 暴露纯函数，便于单元测试。

- `tools/perf/perf-guide.mjs`
  - 新增 `distributed-plan`、`distributed-worker`、`distributed-report` 子命令。
  - 复用现有 verifier、side-effect verifier 和 accuracy trace expectation。

- `tools/perf/*.test.mjs`
  - 覆盖分片、防重复、worker 缺失、summary 不一致、全局分位、verifier 失败和 report 拒绝逻辑。

文档改动：

- 新增 `docs/stressTest/distributed-capacity-runbook.md`。
- 更新 `docs/stressTest/README.md`，明确本地单机和多机入口。
- 更新 `docs/stressTest/report-template.md`，增加多机拓扑、worker 列表、backend 节点列表、全局分位和证据清单。
- 更新 `tools/perf/README.md`，说明底层多机命令只通过 guide 使用。

## 13. 测试策略

单元测试必须覆盖：

- `seqStart/count` 生成全局序号。
- 多 worker 区间无重叠、无缺口。
- 重复 worker ID、缺失 worker、`perfRunId` 不一致时失败。
- 汇总 sent/success/failed/duration。
- 合并 latency buckets 后计算 p95/p99。
- 缺少 latency buckets 时拒绝容量报告。
- distributed accuracy 调用 trace verifier 和 side-effect verifier。
- `perf-guide` 新子命令参数解析和失败状态传播。

手工验证至少运行：

```bash
node --test tools/perf/*.test.mjs
node tools/perf/perf-guide.mjs doctor
```

如果本地依赖可用，再用两个本地 worker 目录模拟多压测机，执行一次小流量 distributed accuracy。

