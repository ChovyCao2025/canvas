# 全链路压测、容量外推与并发准确性验证设计

## 背景

当前系统是 Spring Boot WebFlux 后端，核心执行链路包括 HTTP 事件上报、RocketMQ 触发、直调执行、Disruptor 削峰、DAG 节点执行、MySQL/Redis 持久化、外部 API 调用和人群计算。项目已经具备基础可观测性：

- Actuator / Prometheus 指标。
- `canvas_execution` 执行记录。
- `canvas_execution_trace` 节点轨迹。
- `canvas_execution_request` 异步执行请求表。
- `canvas_execution_dlq` 失败落库。
- Disruptor publish / overflow 指标。
- MySQL、Redis、RocketMQ 和 WireMock 本地依赖。

现在需要一套完整压测体系，回答两个问题：

1. 本机能承载多少压力，瓶颈在哪里，如何保守外推到生产规格。
2. 高并发下数据是否准确，是否存在丢失、重复、错误幂等、上下文串写、ACK 后无落账等问题。

## 目标

1. 覆盖 HTTP 事件上报、RocketMQ 触发、直调执行、人群计算、混合负载、背压和长稳压测。
2. 每次压测都有独立 `perfRunId`，可以隔离查询输入、入口、执行和结果。
3. 自动输出压测报告，包括吞吐、延迟、错误率、资源水位、积压、准确性对账和容量外推。
4. 并发准确性验证必须能证明：
   - 非预期丢失为 0。
   - 重复执行为 0。
   - 错误幂等为 0。
   - 人群计算结果规模符合固定数据集预期。
   - 背压场景不出现上游 ACK 后无执行落账。
5. 尽量少改业务代码，只补必要的压测批次追踪、测试模式和校验查询能力。

## 非目标

1. 不在首期搭建完整生产仿真集群。
2. 不把 `perfRunId` 作为 Prometheus label，避免高基数指标污染。
3. 不重构 DAG 执行内核、节点调度策略或 MQ 消费架构。
4. 不用本机结果直接线性宣称生产容量，必须经过瓶颈模型和安全系数折算。
5. 不在真实生产业务数据中混入压测数据；若未来做生产影子压测，必须使用独立路由、独立用户段和独立清理策略。

## 推荐方案

采用“外部发压 + 少量业务观测字段 + 自动对账校验”的混合压测体系。

纯黑盒压测只能得到请求成功率和大致吞吐，无法可靠证明并发下没有丢失、重复和错误幂等。完整生产仿真环境外推误差更小，但当前生产规格未定，成本过高。混合方案适合当前阶段：本机先建立单实例基线和准确性保障，再把生产机器规格、实例数和依赖规格作为参数输入容量模型。

## 总体架构

### 1. Load Driver

负责造数和发压，生成输入账。

首期建议放在 `tools/perf`，包含：

- HTTP driver：压 `/canvas/events/report`、`/canvas/execute/direct/{canvasId}`、人群计算触发接口。
- MQ driver：向 `CANVAS_MQ_TRIGGER` 按 tag 写消息。
- Dataset loader：写入压测画布、事件定义、MQ 路由、人群测试数据。
- Run manifest：记录本次压测参数、机器规格、依赖规格、场景配置和预期结果。

HTTP 发压可以选择 k6 或 Java CLI。MQ 发压建议使用 Java CLI，复用 RocketMQ 客户端，避免用 shell 或脚本拼接造成客户端行为不一致。

### 2. Perf Scenario

每个场景必须固定输入和固定期望：

- 输入条数。
- 用户数和用户分布。
- 画布数和订阅关系。
- 每条输入的唯一键。
- 预期执行次数。
- 预期下游调用次数。
- 预期人群规模。
- 允许失败类型。

压测不能依赖随机业务逻辑判断是否正确。允许生成随机数据，但必须把随机种子写入 manifest，保证可复现。

### 3. Observability

继续使用 Actuator / Prometheus 看系统资源和趋势：

- HTTP 请求吞吐、错误率、延迟。
- JVM CPU、heap、GC。
- Hikari 活跃连接、等待连接、获取连接耗时。
- Redis 操作耗时和错误。
- RocketMQ 消费速率、积压和重试。
- Disruptor published / overflow。
- DAG 执行成功、失败、重试、DLQ。
- 节点执行耗时。

`perfRunId` 不进入 Prometheus label。单次压测批次维度通过 SQL 和 verifier 查询。

### 4. Correctness Verifier

压测结束后自动对账，生成准确性报告。

四本账：

1. 输入账：Load Driver 记录计划发送、实际发送、发送成功、发送失败。
2. 入口账：服务端实际接收和入队数量。
3. 执行账：系统实际执行数量、成功、失败、暂停、DLQ、幂等拦截、并发拒绝。
4. 结果账：下游 mock 收到数量、节点 trace、最终业务结果、人群规模。

报告必须明确区分：

- 预期失败：例如专门压下游限流时产生的受控失败。
- 非预期失败：例如 DB 异常、ACK 后无落账、执行缺失。
- 未完成：压测窗口结束时仍在重试或积压中，不能按成功统计。

### 5. Capacity Report

根据本机结果和生产参数输出保守容量。

容量取最小瓶颈：

```text
production_capacity =
min(
  app_cpu_capacity,
  db_write_capacity,
  redis_ops_capacity,
  rocketmq_capacity,
  downstream_api_capacity,
  disruptor_worker_capacity
) * safety_factor
```

第一版 `safety_factor = 0.5`。当预发环境或小规模生产仿真校准后，可以调整到 `0.65` 到 `0.75`。

## 最小业务改动

首期允许小改业务代码，但不改核心执行逻辑。

### perfRunId 贯穿

增加 nullable 字段并建索引：

- `event_log.perf_run_id`
- `canvas_execution.perf_run_id`
- `canvas_execution_request.perf_run_id`
- `canvas_execution_dlq.perf_run_id`
- `async_task.perf_run_id`，如果异步任务表已落地

不需要给 `canvas_execution_trace` 增加 `perf_run_id`。trace 通过 `execution_id` join `canvas_execution` 即可。

### 提取规则

`perfRunId` 从输入 payload 中提取：

- HTTP 事件：`attributes.perfRunId`
- MQ 消息：`payload.perfRunId`
- 直调执行：`inputParams.perfRunId`
- 人群计算：请求体或任务上下文中的 `perfRunId`

如果没有 `perfRunId`，按普通业务流量处理，字段为空。

### 唯一输入键

每条输入必须带稳定唯一键：

- HTTP 事件：`perfInputId = perfRunId + ":event:" + seq`
- MQ 消息：`msgId = perfRunId + ":mq:" + seq`
- 直调执行：`idempotencyKey = perfRunId + ":direct:" + seq`
- 人群计算：`taskKey = perfRunId + ":audience:" + audienceId + ":" + datasetVersion`

这些键用于检测重复执行、错误幂等和丢失。

### 压测专用数据

创建压测专用画布、事件、MQ tag 和人群数据集：

- 名称统一以 `PERF_` 开头。
- 用户 ID 统一以 `perf_user_` 开头。
- 事件编码统一以 `PERF_` 开头。
- MQ tag 统一以 `PERF_` 开头。
- 下游调用统一打到 WireMock 或压测 mock endpoint。

清理脚本只删除 `perfRunId` 或 `PERF_` 命名空间内的数据。

## 压测场景矩阵

### 场景 1：HTTP 事件上报

入口：`POST /canvas/events/report`

目标：

- 测 HTTP 接收能力、事件定义校验、路由查询、Disruptor 发布能力。
- 验证事件上报在高并发下不丢、不重复。

默认配置：

- 10000、50000、100000 三档输入。
- 1000 个用户均匀分布。
- 1 个事件编码匹配 1 个压测画布。
- 另有 1 个事件编码不匹配任何画布，用于验证无路由 ACK 逻辑。

准确性：

```text
sent_success == event_log_count
expected_executions == event_log_count * matched_canvas_count
actual_executions + expected_rejections + expected_failures == expected_executions
unexpected_loss == 0
duplicate_execution == 0
```

### 场景 2：RocketMQ 触发

入口：`CANVAS_MQ_TRIGGER` topic + `PERF_*` tag

目标：

- 测 RocketMQ producer、consumer、`canvas_execution_request` 入队、Disruptor 异步执行。
- 验证 MQ 重试、请求状态流转和 DLQ 可对账。

默认配置：

- 10000、50000、100000 三档消息。
- 1、4、8 个 tag 三档。
- 每条消息唯一 `sourceMsgId`。
- 每个 tag 绑定 1 个画布。

准确性：

```text
mq_send_success == canvas_execution_request_count
expected_executions == canvas_execution_request_count
actual_executions + request_failed + request_retry_pending == expected_executions
duplicate_source_msg_id == 0
ack_without_request == 0
```

### 场景 3：直调执行

入口：`POST /canvas/execute/direct/{canvasId}`

目标：

- 测同步执行链路延迟、p95/p99、DB 写入、DAG 执行和幂等行为。

默认配置：

- 1000、5000、10000 三档请求。
- 1 个轻量 DAG：DIRECT_CALL -> IF_CONDITION -> terminal。
- 并发重复请求占比 1%，使用相同 `idempotencyKey` 验证幂等。

准确性：

```text
unique_idempotency_key_count == actual_non_dedup_execution_count
intentional_duplicate_count == deduplicated_count
unexpected_dedup == 0
duplicate_execution_for_same_key == 0
```

### 场景 4：人群计算

入口：`POST /canvas/audiences/{id}/compute`

目标：

- 测批量计算耗时、异步任务状态、人群规模准确性。
- 验证并发重复触发不会创建重复运行任务。

默认配置：

- 固定 10000、100000、1000000 三档 demo 用户数据。
- 固定规则：例如金额、渠道、时间窗口、标签组合。
- 每个规则预先计算 expected count。

准确性：

```text
actual_audience_count == expected_audience_count
duplicate_running_task == 0
task_status in (SUCCEEDED, FAILED_WITH_EXPECTED_REASON)
```

### 场景 5：混合压测

入口：

- HTTP 事件上报
- MQ 触发
- 直调执行
- 人群计算

目标：

- 验证资源争用下各链路的隔离能力。
- 观察 DB 连接池、Redis、Disruptor、RocketMQ 消费线程和下游 mock 的竞争。

默认流量占比：

- HTTP 事件：50%
- MQ 触发：30%
- 直调执行：15%
- 人群计算：5%

准确性：

每条链路分别对账，不允许用整体成功率掩盖单链路丢失。

### 场景 6：背压和故障注入

目标：

- 验证 Ring Buffer 满、DB 慢、Redis 慢、下游慢、下游限流时系统行为可解释、可对账。

故障类型：

- WireMock 延迟 1s、3s、5s。
- API_CALL rate limit 超限。
- Redis 短暂不可用。
- DB 连接池缩小到低水位。
- Disruptor ring buffer 调小到压测档。

准确性：

```text
accepted_without_ledger == 0
ack_without_execution_request == 0
overflow_count is explained
dlq_count is explained
retry_pending_count is explained
```

### 场景 7：长稳压测

目标：

- 验证 30 到 120 分钟持续压力下无内存爬升、连接泄漏、积压持续增长。

默认配置：

- 以峰值容量的 50% 持续 30 分钟。
- 以峰值容量的 70% 持续 30 分钟。
- 压测停止后等待积压清空。

通过标准：

- JVM heap 使用没有单调不可回收增长。
- DB 连接池无持续等待。
- RocketMQ 和 Disruptor backlog 可清空。
- 错误率保持在阈值内。
- 准确性对账通过。

## 并发准确性验证

### 丢失检测

非预期丢失定义：

```text
unexpected_loss =
expected_executions
- actual_success
- actual_failed_with_record
- actual_dlq
- actual_rejected_with_record
- actual_retry_pending
```

通过标准：`unexpected_loss == 0`。

### 重复检测

对唯一输入键分组：

- HTTP 事件按 `perfInputId`。
- MQ 按 `sourceMsgId`。
- 直调按 `idempotencyKey`。
- 人群计算按 `taskKey`。

任何非预期输入键产生多次有效执行，判定为重复执行。

### 错误幂等检测

错误幂等定义：不同输入键被系统当成重复请求拦截。

检测方式：

```text
unexpected_dedup =
dedup_count
- intentional_duplicate_count
```

通过标准：`unexpected_dedup == 0`。

### 上下文串写检测

同一 `canvasId + userId` 并发触发是高风险场景，因为当前执行上下文按 `canvasId + userId` 存取。

压测必须包含：

- 同一用户同一画布并发不同 payload。
- 同一用户同一画布存在挂起/恢复时再次触发。
- 不同用户同一画布并发。
- 同一用户不同画布并发。

校验方式：

- 每条输入带 `perfInputId` 和 `expectedValue`。
- 节点 trace output 或最终 result 中必须能找回对应 `perfInputId`。
- 若 A 输入的 result 出现 B 输入的 payload，判定为上下文串写。

### ACK 后无落账检测

MQ 和异步入口必须保证“接受就有账”。

MQ 场景：

```text
mq_send_success == canvas_execution_request_count + parse_failed_with_record
```

如果 consumer ACK 了消息，但没有 `canvas_execution_request`、执行记录、DLQ 或明确失败记录，判定为严重错误。

HTTP 异步场景：

```text
http_2xx == event_log_count
event_log_count * matched_canvas_count == execution_or_rejection_ledger
```

### 人群结果准确性

人群计算不能只看任务成功，要看结果规模。

固定数据集必须提供：

- datasetVersion
- totalUsers
- expectedCountByRule
- seed

压测报告输出：

```text
audienceId
datasetVersion
expectedCount
actualCount
diff
verdict
```

通过标准：`diff == 0`。

## 默认验收阈值

首期默认阈值：

```text
unexpected_loss = 0
duplicate_execution = 0
unexpected_dedup = 0
wrong_audience_count = 0
ack_without_ledger = 0
error_rate < 0.1%
HTTP intake p95 < 300ms
CPU < 75%
JVM heap < 75%
DB connection pending ~= 0
Redis timeout rate = 0
Disruptor overflow = 0 in normal tests
RocketMQ backlog clears after load stops
```

故障注入场景可以允许 overflow、DLQ、retry 或限流失败，但必须全部可解释、可对账。

## 容量外推模型

### 本机基线采集

每个场景采集：

- app CPU cores
- app memory
- JVM heap
- DB 连接池大小
- Redis 连接池大小
- RocketMQ consumer thread count
- Disruptor ring buffer size
- Disruptor worker count
- 每秒输入数
- 每秒执行数
- p50 / p95 / p99
- DB writes per execution
- Redis ops per execution
- downstream calls per execution
- node count per execution
- failure and retry ratio

### 计算方式

应用 CPU 容量：

```text
app_cpu_capacity =
local_stable_qps
* (prod_app_cores_total / local_app_cores)
* cpu_efficiency_factor
```

第一版 `cpu_efficiency_factor = 0.75`。

DB 写入容量：

```text
db_write_capacity =
prod_db_safe_write_qps / writes_per_business_event
```

Redis 容量：

```text
redis_ops_capacity =
prod_redis_safe_ops / redis_ops_per_business_event
```

RocketMQ 容量：

```text
rocketmq_capacity =
min(prod_topic_queue_count, prod_consumer_instances * consume_threads_per_instance)
* stable_msg_per_queue_per_sec
```

下游 API 容量：

```text
downstream_api_capacity =
downstream_rate_limit_per_sec / downstream_calls_per_business_event
```

最终容量：

```text
safe_capacity =
min(app_cpu_capacity, db_write_capacity, redis_ops_capacity, rocketmq_capacity, downstream_api_capacity)
* safety_factor
```

第一版 `safety_factor = 0.5`。

### 报告输出

示例：

```text
local_stable_event_qps=1200
local_p95=180ms
bottleneck=DB_WRITE
prod_app_instances=4
prod_app_cores_total=32
estimated_raw_capacity=9600/s
safety_factor=0.5
recommended_capacity=4800/s
recommended_alert_threshold=70% of recommended_capacity
```

报告必须写清楚“瓶颈来自哪里”，不能只给一个 QPS 数字。

## 数据清理

清理脚本按以下维度删除：

- `perf_run_id = ?`
- `user_id like 'perf_user_%'`
- `event_code like 'PERF_%'`
- `canvas.name like 'PERF_%'`
- MQ tag / route key like `PERF_%`

清理前先输出将删除的行数，清理后输出剩余行数。默认不清理非 `PERF_` 和非 `perfRunId` 数据。

## 风险与缓解

### 风险：本机外推误差过大

缓解：报告只给保守容量，默认安全系数 0.5，并明确瓶颈。生产规格明确后用预发或小规模仿真重新校准。

### 风险：压测污染业务数据

缓解：所有数据使用 `perfRunId` 和 `PERF_` 命名空间，清理脚本只操作压测数据。

### 风险：Prometheus 高基数

缓解：`perfRunId` 不进入指标 label。批次级查询通过 SQL 和 verifier 完成。

### 风险：并发上下文串写难定位

缓解：专门设计同一用户同一画布并发场景，每条输入带 `perfInputId`，并要求 trace/result 可查回该输入。

### 风险：故障注入导致误判失败

缓解：manifest 标明本场景允许的失败类型。verifier 区分 expected failure 和 unexpected failure。

## 实施边界

首期实施内容：

1. 增加 `perf_run_id` nullable 字段、索引和实体字段。
2. 从 payload 提取 `perfRunId` 并贯穿到执行、请求、DLQ 和任务记录。
3. 新增 `tools/perf`，包含数据准备、HTTP/MQ 发压、verifier 和报告生成。
4. 新增压测专用画布、事件、MQ tag、人群数据集初始化脚本。
5. 新增 README，说明本机压测、准确性校验和容量报告使用方式。

不在首期实施：

1. 生产影子压测。
2. 多机自动编排压测集群。
3. Grafana dashboard 自动生成。
4. 对 DAG 执行引擎做性能重构。

## 验收标准

完成后应能执行以下流程：

```text
1. 初始化压测数据
2. 启动本地依赖和后端
3. 运行 HTTP 事件压测
4. 运行 MQ 压测
5. 运行直调压测
6. 运行人群计算压测
7. 运行混合压测
8. 运行 verifier
9. 生成容量报告
10. 清理压测数据
```

每个场景报告至少包含：

- `perfRunId`
- 场景名
- 机器规格
- 压测参数
- planned / sent / accepted / executed / succeeded / failed / dlq / retry / rejected
- p50 / p95 / p99
- 资源水位
- 准确性 diff
- 容量外推结论
- PASS / FAIL verdict

首期总体验收：

```text
所有 normal 场景:
  unexpected_loss = 0
  duplicate_execution = 0
  unexpected_dedup = 0
  ack_without_ledger = 0
  verifier verdict = PASS

所有 fault 场景:
  所有失败都有明确记录
  所有重试、DLQ、限流、overflow 都可解释
  verifier verdict = PASS_WITH_EXPECTED_FAILURES
```

