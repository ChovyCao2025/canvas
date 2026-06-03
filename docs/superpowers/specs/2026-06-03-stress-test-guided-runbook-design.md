# Canvas 压测方案重构与向导设计

## 1. 背景

当前项目已有本机容器化容量压测文档和 `tools/perf` 脚本，覆盖 direct、event、MQ、audience、verifier、threshold、capacity report、cleanup 等能力。整体方向是合理的：固定后端容器资源、发压端留在宿主机、每轮使用 `perfRunId` 隔离数据、先 smoke 再阶梯压测、用 verifier 对账后再做容量外推。

但当前方案不能直接交给非专业用户执行。主要问题是执行入口分散、手工步骤过长、event 压测缺 HMAC 签名支持、部分报告文档容易被误读为实测结论，并且监控快照和报告证据没有被脚本统一收口。重构目标是把压测从“懂代码的人手工拼命令”改成“按向导执行、失败即停、数据可追溯”的流程。

## 2. 审计结论

当前方案合理的部分：

- 使用本机 Docker 固定后端 CPU/内存资源，不把本机结果直接等价为生产容量。
- 每轮压测使用 `perfRunId`，并在 `event_log`、`canvas_execution`、`canvas_execution_request`、`canvas_execution_dlq`、`audience_compute_run` 中保留压测账本。
- `tools/perf/verifier.mjs` 会检查丢失、重复执行、异常去重、未完成 retry、DLQ、失败执行和 audience count，不只看 HTTP 成功数。
- `threshold-runner.mjs` 支持逐档停止，能避免继续使用已经不稳定的档位。
- `capacity-report.mjs` 拒绝 verifier 为 `FAIL` 的输入，容量外推方向正确。

当前方案不合理或不足的部分：

- `POST /canvas/events/report` 当前由 `EventReportAuthService` 强制校验 `X-Canvas-Timestamp` 和 `X-Canvas-Signature`，但 `perf-runner.mjs` 的 event 模式没有生成 HMAC 头。按现有文档执行 event smoke 会因缺少签名头返回 401。
- 文档以长命令块为主，缺少统一入口、前置检查、失败原因解释和下一步提示；非专业用户很容易漏掉环境变量、路由发布、MQ broker 地址、verifier 参数或 cleanup 范围。
- `docs/stressTest/老板汇报版-并发评估摘要.md` 和 `docs/stressTest/并发量评估报告.md` 包含容量结论口径，但没有绑定具体 `perfRunId`、runner summary、verifier JSON 和监控快照，不能作为实测报告入口。
- 长稳压测以固定 count 模型表达，文档要求至少 30 分钟，但没有脚本保障实际持续时间和采样证据。
- 监控采集仍依赖用户手工执行多条命令，报告无法强制证明 CPU、GC、Hikari、Redis、RocketMQ、MySQL 状态是否稳定。
- cleanup 有“账本清理”和“全量清理”两种语义，但底层脚本默认 SQL 会删除 `PERF_%` event/MQ fixture definition；向导必须防止用户误删 fixture 后继续跑旧路由。
- 阶梯脚本当前只覆盖 direct/event；MQ 和长稳必须统一到向导层。audience 压测作为显式 profile 支持，不进入默认容量 gate。

## 3. 目标

本次重构交付一套完整压测方案：

- 非专业用户从一个入口开始，可以完成环境检查、fixture 准备、smoke、阶梯压测、长稳压测、报告生成和清理。
- event 压测自动携带 HMAC 签名，不再要求用户手工计算签名。
- 任何准确性失败都不能进入容量外推。
- 报告必须能追溯到 `perfRunId`、runner summary、verifier JSON、监控快照、环境信息、外推参数和 cleanup 记录。
- 不保留会误导执行的旧方案入口；历史口径文档要删除或重构为新结构的一部分。

## 4. 非目标

- 不在本阶段承诺真实生产容量数字。生产容量必须来自实际压测报告和目标环境参数。
- 不把 Docker 端到端压测放入单元测试；单元测试覆盖脚本逻辑，端到端执行由 runbook 和向导完成。
- 不重构后端执行引擎、MQ 消费模型、数据库表结构或容量治理代码。
- 不将本机测试结果自动等同于云上或生产集群结果。

## 5. 文档结构

重构后 `docs/stressTest` 只保留一个清晰入口：

- `docs/stressTest/README.md`
  - 唯一入口。
  - 写明适用范围、禁止事项、执行顺序、结果可信标准。
  - 指向 runbook、审计报告和报告模板。

- `docs/stressTest/local-capacity-runbook.md`
  - 完整本机容量压测手册。
  - 以 `perf-guide.mjs` 为主路径，底层命令只作为排障附录。
  - 覆盖 doctor、fixture、smoke、threshold、soak、report、cleanup。

- `docs/stressTest/performance-audit.md`
  - 当前方案审计报告。
  - 记录为什么重构、旧方案哪些步骤不再适用、准确性风险是什么。

- `docs/stressTest/report-template.md`
  - 标准报告模板。
  - 缺少必要证据时输出“数据不完整”，不能输出容量结论。

旧的 `docs/stressTest/2026-05-27-local-container-capacity-testing-design.md` 不再作为执行入口，实施时由 `local-capacity-runbook.md` 替代。当前老板汇报版和并发评估报告不绑定实测证据，实施时从 `docs/stressTest` 删除；后续需要汇报材料时，只能从 `report-template.md` 和实测 run 目录生成。

## 6. 脚本结构

新增 `tools/perf/perf-guide.mjs`，作为非专业用户入口。底层脚本继续保留，职责如下：

- `perf-guide.mjs`
  - 统一向导 CLI。
  - 子命令：`doctor`、`fixture`、`smoke`、`threshold`、`soak`、`report`、`cleanup`。
  - 每个子命令输出下一步提示、失败原因和证据文件位置。
  - 危险操作默认 dry-run，执行必须显式传 `--execute true` 或 `--rebuild true`。

- `perf-runner.mjs`
  - 保留底层 HTTP 发压。
  - event 模式新增 HMAC 支持：从 `--event-secret` 或 `PERF_EVENT_SECRET` 读取密钥，自动生成 `X-Canvas-Timestamp` 和 `X-Canvas-Signature`。
  - summary 增加签名配置摘要，只记录是否启用，不记录密钥。

- `threshold-runner.mjs`
  - 透传 event HMAC 参数。
  - 保持失败即停。
  - 输出每一档 summary/verifier 路径，供 report 汇总。

- `verifier.mjs`
  - 保持准确性判定核心。
  - `PASS_WITH_EXPECTED_FAILURES` 只允许故障演练使用，不允许普通容量报告使用。

- `capacity-report.mjs`
  - 保持拒绝 `FAIL`。
  - 向导层进一步拒绝普通容量报告使用 `PASS_WITH_EXPECTED_FAILURES`。

- `cleanup.mjs`
  - 保留底层清理能力。
  - 向导层区分 `ledger` 和 `all` 两种清理范围，默认只清当前 `perfRunId` 账本。

## 7. 向导命令设计

### 7.1 doctor

`doctor` 检查执行环境，不创建业务数据：

- Java 21、Maven 3.9+、Node.js 18+、Docker CLI 可用。
- Docker Desktop 可用资源满足当前 runbook 最低要求。
- 本地端口 8080、8099、3306、6379、9876 状态清晰。
- `docker-compose.local.yml` 依赖容器可启动或已启动。
- 后端健康检查、Actuator、MySQL、Redis、WireMock、RocketMQ 可访问。
- 数据库包含 `perf_run_id` 相关列和 `audience_compute_run` 表。
- `node --test tools/perf/*.test.mjs` 通过。
- `PERF_EVENT_SECRET` 存在且长度满足 HMAC 要求；后端容器的 `CANVAS_EVENT_REPORT_SECRET` 必须与它一致。

### 7.2 fixture

`fixture` 创建或重建压测 fixture：

- 创建 `PERF_ORDER_PAID` event definition。
- 创建 `PERF_MQ` MQ definition。
- 创建 direct、event、MQ 轻链路画布。
- 通过 API 发布画布，确保事件和 MQ 路由注册到 Redis。
- 校验同一个 event code / MQ tag 只对应预期数量的已发布画布。
- 默认复用 fixture；重建必须显式 `--rebuild true`，并先下线旧 `PERF_` 画布。

### 7.3 smoke

`smoke` 先验证正确性，不测试吞吐：

- direct：小流量 direct run + verifier。
- event：小流量 event run，自动 HMAC 签名 + verifier。
- MQ：发送少量 MQ 消息 + verifier；如果 broker 地址不可达，输出修复命令和检查项并停止 MQ 场景。
- 任一场景 verifier 不是 `PASS`，后续容量压测不得继续。

### 7.4 threshold

`threshold` 执行阶梯压测：

- 支持 direct/event 主路径，MQ 作为单独 profile 执行。
- 每档生成独立 `perfRunId`、summary、verifier、监控快照。
- 停止条件：HTTP failed 超阈值、p95 超阈值、verifier 非 `PASS`、运行中监控发现明显资源恶化。
- 输出 `MAX_STAGE_STABLE`、`THRESHOLD_FOUND` 或 `NO_STABLE_STAGE`，并记录最后稳定档。

### 7.5 soak

`soak` 执行长稳：

- 目标 QPS 使用阶梯压测最后稳定值的 70%。
- 运行时间必须达到 `--min-duration-min`，默认 30 分钟。
- 如果底层 runner 仍是固定 count 模型，向导根据上一档 QPS 估算 count，并在运行后校验实际 duration。
- 长稳期间每 30 秒采集容器、Actuator、MySQL、Redis、RocketMQ 快照。
- duration 不足、verifier 非 `PASS` 或 backlog 持续增长时，本轮长稳无效。

### 7.6 report

`report` 生成报告：

- 只读取向导生成的 run 目录。
- 普通容量报告只接受 verifier `PASS`。
- 报告包含环境、fixture、runner summary、verifier、监控快照、稳定档、长稳结论、外推参数、瓶颈候选、cleanup 状态。
- 缺少必要证据时，不输出容量数字，只输出数据缺失清单。

### 7.7 cleanup

`cleanup` 清理数据：

- 默认 `--scope ledger`，只删除当前 `perfRunId` 相关账本数据。
- `--scope all` 才允许删除 `PERF_%` event/MQ definition 和 fixture 相关数据。
- `--execute true` 才真正执行；默认只打印将删除的对象和数量。

## 8. 数据流

标准数据流：

```text
doctor
  -> fixture
  -> smoke
  -> threshold
  -> soak
  -> report
  -> cleanup
```

每个 run 写入一个 run 目录：

```text
tmp/perf-runs/<perfRunId>/
  runner-summary.json
  verifier.json
  monitor/
  command.json
  cleanup-preview.txt
  cleanup-executed.txt
```

多档 threshold 额外生成汇总：

```text
tmp/perf-threshold/<sessionId>/
  threshold-summary.json
  runs/<perfRunId>/
```

## 9. 准确性规则

- HTTP `success` 只表示入口请求成功，不表示业务执行成功。
- 任何容量结论都必须以 verifier `PASS` 为前置条件。
- `PASS_WITH_EXPECTED_FAILURES` 只能进入故障演练报告，不能进入普通容量外推。
- runner 失败、verifier 失败、DLQ 非预期、retry pending、重复执行、异常去重、未入账 ack 都会使该 run 无效。
- `matchedCanvasCount` 必须来自 fixture 校验结果，不能让用户凭感觉填写。
- cleanup 记录必须进入报告，避免同一 `perfRunId` 残留数据污染下一轮。

## 10. 错误处理

向导必须给出可执行的错误信息：

- 缺工具：输出缺少的命令和最低版本。
- 后端 401：提示检查 `PERF_EVENT_SECRET` 和 `CANVAS_EVENT_REPORT_SECRET` 是否一致。
- event verifier loss：提示检查 event route、published canvas 数量、`matchedCanvasCount`。
- MQ smoke 失败：提示检查 broker `brokerIP1` 和容器可达地址。
- Redis/MySQL 连接失败：输出容器名、端口和下一步检查命令。
- 长稳不足 30 分钟：输出实际 duration，并拒绝报告容量结论。
- cleanup 危险参数缺失：只输出 dry-run，不执行删除。

## 11. 测试策略

单元测试覆盖：

- event HMAC 生成：timestamp + raw body 产生正确 `X-Canvas-Signature`。
- `perf-runner` event 模式在配置密钥时发送签名头，summary 不泄露密钥。
- `threshold-runner` 透传 event secret。
- `perf-guide` 参数解析和子命令路由。
- smoke 失败后不会继续 threshold。
- report 拒绝 verifier `FAIL` 和普通容量场景下的 `PASS_WITH_EXPECTED_FAILURES`。
- cleanup 默认 dry-run，执行必须显式 `--execute true`。
- fixture 重建必须显式 `--rebuild true`。

人工验证覆盖：

- `node --test tools/perf/*.test.mjs` 通过。
- direct smoke 通过。
- event smoke 通过 HMAC 校验并 verifier `PASS`。
- MQ smoke 在 broker 地址正确时 verifier `PASS`。
- 至少一个 direct/event threshold session 能在失败档停止或输出全部稳定。
- 至少一个 soak run 实际 duration 达到 30 分钟并生成监控快照。

## 12. 验收标准

本设计完成后的验收标准：

- `docs/stressTest/README.md` 是唯一压测入口。
- 非专业用户按 runbook 可以从空本地环境完成 doctor、fixture、smoke。
- event 压测不再因缺少 HMAC 头失败。
- verifier 非 `PASS` 的 run 无法进入普通容量报告。
- 报告能追溯每个容量数字的 `perfRunId`、summary、verifier、监控快照和外推参数。
- 旧的不可靠压测入口被删除或重构，不再与新 runbook 并列存在。
- 单元测试全部通过。

## 13. 实施顺序

后续 implementation plan 按以下顺序拆：

1. 重构 `docs/stressTest` 入口和审计报告。
2. 为 `perf-runner` 增加 event HMAC 支持并补测试。
3. 为 `threshold-runner` 透传 event HMAC 参数并补测试。
4. 新增 `perf-guide.mjs` 的 `doctor`、`smoke`、`report`、`cleanup` 基础能力。
5. 新增 `fixture`、`threshold`、`soak` 向导能力。
6. 删除或重构旧的不可靠文档入口。
7. 跑单元测试和最小 smoke 验证。
