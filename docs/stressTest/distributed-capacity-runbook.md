# 多机容量压测操作手册

这是多台压测机或多后端节点场景的执行手册。单机流程仍以 [本地容量压测操作手册](./local-capacity-runbook.md) 为准；多机压测必须在单机 `smoke` 和 `accuracy` 都通过后执行。

多机容量数字只有在 `distributed-report` 返回 `status: "PASS"`，并且全局 verifier 为 `PASS` 时才有效。不要人工相加多台压测机的 QPS、p95 或成功数后发布容量结论。

## 1. 适用拓扑

### 多台压测机打一个后端入口

```text
load-worker-01 \
load-worker-02  -> http://backend-or-lb:8080 -> canvas-engine
load-worker-03 /
```

这个拓扑用于排除单台压测机发不出足够流量的问题。`baseUrl` 可以是单个后端，也可以是负载均衡入口。

### 多台压测机打多后端节点

```text
load-worker-01 \
load-worker-02  -> http://lb:8080 -> canvas-engine-01 \
load-worker-03 /                    canvas-engine-02  -> MySQL / Redis / RocketMQ / WireMock
                                      canvas-engine-03 /
```

这个拓扑用于验证后端横向扩容。所有 `canvas-engine` 节点必须连接同一个 MySQL、Redis、RocketMQ 和 WireMock，否则 verifier 无法读取完整账本。

## 2. 停止条件

出现以下任一情况，立即停止并只整理失败报告：

- 任一 worker 的 `runner-summary.json` 缺失。
- 任一 worker 的 `failed` 大于 0。
- worker 的 `perfRunId`、`workerId`、`seqStart` 或 `seqCount` 与 plan 不一致。
- 分片序号重叠或有缺口。
- `distributed-report` 拒绝本次运行。
- 全局 verifier 不是 `PASS`。
- 多机 accuracy 的 side-effect verifier 不是 `PASS`。
- 任一后端节点重启、健康检查失败或 OOM。
- MySQL、Redis、RocketMQ、WireMock 持续报错。
- 压测期间没有采集压测机、后端节点和共享依赖的监控。

不要把失败、不完整或证据不足的运行整理成容量数字。

## 3. 前置检查

在控制机执行：

```bash
cd /Users/photonpay/project/canvas
node --test tools/perf/*.test.mjs
node tools/perf/perf-guide.mjs doctor
```

先按本地 runbook 完成：

```text
fixture -> smoke -> accuracy
```

`accuracy` 必须是 `PASS`，且 trace verifier 和 side-effect verifier 都是 `PASS`，才能开始多机压测。

所有机器都必须：

- 能访问同一个 `BASE_URL`。
- 能访问同一个代码版本或同一个压测工具包。
- 设置相同的 `PERF_EVENT_SECRET`。
- 系统时间已经同步。建议所有机器执行 `date -u`，时间差超过 2 秒时先修复 NTP。

## 4. 多后端节点要求

如果 `BASE_URL` 是负载均衡入口，开始前确认：

- 所有后端节点运行同一个镜像或同一个 git commit。
- 所有后端节点设置相同的 `CANVAS_EVENT_REPORT_SECRET`。
- 所有后端节点连接同一组 MySQL、Redis、RocketMQ、WireMock。
- LB 健康检查能摘除不可用节点。
- LB 地址、节点列表、分发策略、连接复用策略写入报告。
- 每个后端节点都单独采集 CPU、内存、GC、线程、Hikari、Disruptor 指标。

## 5. 控制机生成 plan

设置机器清单。`workerId` 必须稳定、唯一，并且和后续每台压测机执行命令里的值一致。

```bash
export BASE_URL=http://lb-or-backend:8080
export WORKER_IDS=worker-01,worker-02,worker-03
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_dist_threshold_s1
export TOTAL_COUNT=300000
export TOTAL_CONCURRENCY=300
```

生成 event 压测 plan：

```bash
node tools/perf/perf-guide.mjs distributed-plan \
  --base-url "$BASE_URL" \
  --perf-run-id "$PERF_RUN_ID" \
  --mode event \
  --event-code PERF_ORDER_PAID \
  --event-secret-env PERF_EVENT_SECRET \
  --matched-canvas-count "$MATCHED_CANVAS_COUNT" \
  --worker-ids "$WORKER_IDS" \
  --total-count "$TOTAL_COUNT" \
  --total-concurrency "$TOTAL_CONCURRENCY" \
  --distributed-root tmp/perf-distributed | tee "tmp/perf-distributed-$PERF_RUN_ID-plan-output.json"
```

plan 文件位置：

```bash
export PLAN_FILE="tmp/perf-distributed/$PERF_RUN_ID/plan.json"
test -f "$PLAN_FILE"
```

plan 会把总请求数和总并发切分到每台 worker，并为每台 worker 分配不重叠的全局 `seq` 区间。不要手工修改 `seqStart`、`count` 或 `concurrency`。

## 6. 分发 plan 并执行 worker

把 `plan.json` 分发到每台压测机。每台压测机执行自己的 `workerId`：

```bash
cd /Users/photonpay/project/canvas
export PERF_EVENT_SECRET="<same-secret-as-backend>"
node tools/perf/perf-guide.mjs distributed-worker \
  --plan-file "$PLAN_FILE" \
  --worker-id worker-01
```

其他机器分别使用 `worker-02`、`worker-03`。每台机器会生成：

```text
tmp/perf-distributed/<perfRunId>/workers/<workerId>/runner-summary.json
```

执行完成后，把每台压测机的 `runner-summary.json` 收集回控制机对应目录：

```text
tmp/perf-distributed/<perfRunId>/workers/worker-01/runner-summary.json
tmp/perf-distributed/<perfRunId>/workers/worker-02/runner-summary.json
tmp/perf-distributed/<perfRunId>/workers/worker-03/runner-summary.json
```

不要改文件内容。`distributed-report` 会校验 worker 数量、序号区间、成功数、失败数和延迟桶。

## 7. 控制机执行 distributed-report

```bash
node tools/perf/perf-guide.mjs distributed-report \
  --plan-file "$PLAN_FILE" \
  --wiremock-url http://localhost:8099
```

返回必须是 `status: "PASS"`。该命令会生成：

```text
tmp/perf-distributed/<perfRunId>/distributed-summary.json
tmp/perf-distributed/<perfRunId>/verifier.json
tmp/perf-distributed/<perfRunId>/side-effect-verifier.json    # 仅 accuracy 场景
```

`distributed-summary.json` 里的 p95/p99 来自所有 worker 的 `latencyBuckets` 合并结果，不是各 worker p95 的平均值。

## 8. 多机 accuracy

多机容量测试前，建议用 `PERF_ENGINE_ACCURACY` 再跑一轮分布式准确性验证。这个场景不要求 30 分钟持续时间，但必须通过全局账本、trace 和 side-effect 校验。

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_dist_accuracy
node tools/perf/perf-guide.mjs distributed-plan \
  --base-url "$BASE_URL" \
  --perf-run-id "$PERF_RUN_ID" \
  --mode direct \
  --canvas-id "$ENGINE_ACCURACY_CANVAS_ID" \
  --event-secret-env PERF_EVENT_SECRET \
  --worker-ids "$WORKER_IDS" \
  --total-count 60000 \
  --total-concurrency 300 \
  --accuracy true \
  --distributed-root tmp/perf-distributed
```

分发 plan 后，每台 worker 执行 `distributed-worker`。收集所有 summary 后执行：

```bash
node tools/perf/perf-guide.mjs distributed-report \
  --plan-file "tmp/perf-distributed/$PERF_RUN_ID/plan.json" \
  --wiremock-url http://localhost:8099
```

如果 `traceMismatch`、`traceFailed`、`traceDuplicateSuccess`、`traceBufferPending`、`duplicateSideEffects`、`branchMismatch` 任一大于 0，停止容量测试。

## 9. 多机 threshold

多机 threshold 采用多轮 plan。每一档使用新的 `perfRunId`：

```text
s1: TOTAL_COUNT=300000  TOTAL_CONCURRENCY=300
s2: TOTAL_COUNT=600000  TOTAL_CONCURRENCY=600
s3: TOTAL_COUNT=900000  TOTAL_CONCURRENCY=900
```

每一档执行：

```text
distributed-plan -> 所有 distributed-worker -> 收集 summary -> distributed-report
```

某一档失败时停止，上一档是当前稳定档。不要继续更高并发。

## 10. 多机 soak

用 threshold 的最后稳定并发作为输入，选择足够大的 `TOTAL_COUNT`，确保全局持续时间至少 30 分钟：

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)_dist_soak
export TOTAL_CONCURRENCY=<stable-total-concurrency>
export TOTAL_COUNT=<count-large-enough-for-30-minutes>
node tools/perf/perf-guide.mjs distributed-plan \
  --base-url "$BASE_URL" \
  --perf-run-id "$PERF_RUN_ID" \
  --mode event \
  --event-code PERF_ORDER_PAID \
  --event-secret-env PERF_EVENT_SECRET \
  --matched-canvas-count "$MATCHED_CANVAS_COUNT" \
  --worker-ids "$WORKER_IDS" \
  --total-count "$TOTAL_COUNT" \
  --total-concurrency "$TOTAL_CONCURRENCY" \
  --report-type capacity \
  --min-duration-min 30 \
  --distributed-root tmp/perf-distributed
```

如果 `distributed-report` 提示持续时间不足，增大 `TOTAL_COUNT`，换新的 `perfRunId` 重跑。不要降低 `--min-duration-min` 来让报告通过。

## 11. 监控证据

压测期间必须同时采集：

- 每台压测机：CPU、内存、网络、Node.js 版本、机器规格。
- 每个后端节点：CPU、内存、GC、线程、Hikari、Disruptor、Actuator 指标。
- 共享依赖：MySQL 连接数和锁等待、Redis ops 和 rejected/evicted、RocketMQ backlog、WireMock 请求日志。
- LB：后端节点健康状态、连接数、5xx、分发策略。

监控必须在压测期间采集，不能在结束后补采。缺少任一类关键监控时，报告模板中必须写为证据缺口。

## 12. 清理

多机使用同一个全局 `perfRunId`，清理方式和单机一致。在控制机执行：

```bash
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID"
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID" --execute true
```

完整清理只在全部压测结束后使用：

```bash
node tools/perf/perf-guide.mjs cleanup --perf-run-id "$PERF_RUN_ID" --scope all --execute true
```

## 13. 完成检查清单

- 单机 `smoke` 和 `accuracy` 已通过。
- 每台 worker 都使用同一个 `plan.json` 和唯一 `workerId`。
- 所有 worker summary 已收集到控制机。
- `distributed-summary.json` 存在，且 `failed` 为 0。
- `verifier.json` 存在，且 `verdict` 为 `PASS`。
- accuracy 场景的 `side-effect-verifier.json` 存在，且 `verdict` 为 `PASS`。
- 多后端场景已记录 LB 和每个后端节点信息。
- 压测机、后端节点、MySQL、Redis、RocketMQ、WireMock、LB 监控已保存。
- cleanup 预览和执行记录已保存。
