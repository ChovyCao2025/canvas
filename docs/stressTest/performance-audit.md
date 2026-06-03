# 压力测试方案审计

## 结论

旧版压力测试材料包含一些有用的原始命令，但不适合非专业用户直接执行。主要问题是步骤过于依赖人工拼接、默认假设事件请求不需要签名，并且允许没有绑定实测证据的容量叙述。

## 保留的合理部分

- 后端应运行在固定资源容器中，避免资源漂移影响结论。
- 压测流量应从宿主机发起，让后端容器 CPU/内存限制保持有效。
- 使用 `perfRunId` 隔离压测数据是正确做法。
- `verifier.mjs` 不只检查 HTTP 成功，还会核对执行结果。
- 容量估算必须拒绝 verifier `FAIL` 的数据。

## 已移除或修正的阻断问题

- event 和 direct 压测都通过 `--event-secret-env` 支持 HMAC 签名；不接受把密钥值作为命令行参数传入。
- 当前有效文档不再保留缺少 runner/verifier 证据的历史容量结论。
- cleanup 默认只清理 ledger 数据，普通清理不会删除 `PERF_%` 事件和 MQ 定义。
- guide `report` 会检查完整 runner 证据、完整 verifier 证据、匹配的 `perfRunId`、零请求失败，以及容量报告所需的压测时长。
- soak 实际运行时间短于配置要求时，不能用于容量报告。
- verifier 已补充 trace 证据：可以校验每个关键节点的 success、failed、skipped 数量，并拒绝重复成功 trace。
- `side-effect-verifier.mjs` 会审计 WireMock request journal，确认高并发下触达副作用的总数、偶/奇分支数和 `perfInputId + branch` 唯一性。
- `perf-guide fixture --rebuild true` 会通过后端 API 创建并发布标准 `PERF_` 画布，避免新人手工拼接 payload。
- `perf-guide accuracy` 会一次执行 runner、trace verifier 和 side-effect verifier，准确性不通过时不能进入容量测试。

## 当前执行模型

操作手册是执行人的检查清单。所有关键闸门和报告有效性判断都通过 `perf-guide.mjs` 完成。测试资源创建由 `fixture` 命令完成，但必须显式传入 `--rebuild true`，避免误创建或误归档本地 `PERF_` 资源。

只有 accuracy、soak、`tools/perf/perf-guide.mjs report` 都通过，并且 verifier 为 `PASS` 的报告，才能作为有效容量证据。
