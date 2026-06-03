# Canvas 压力测试

本目录是本地容量压测的唯一有效入口。执行人应按 [本地容量压测操作手册](./local-capacity-runbook.md) 操作，并使用 `tools/perf/perf-guide.mjs` 做正确性闸门。

不要把历史归档中的容量估算当成实测结果。一个容量数字只有同时具备以下证据时才有效：

- 唯一的 `perfRunId`
- runner summary JSON
- verifier JSON，且 `verdict: "PASS"`
- 准确性测试的 side-effect verifier JSON，且 `verdict: "PASS"`
- guide `report` 输出，且 `status: "PASS"`
- 压测期间的监控快照
- 环境与资源配置
- 容量估算输入参数
- 清理记录

## 快速开始

```bash
cd /Users/photonpay/project/canvas
node --test tools/perf/*.test.mjs
node tools/perf/perf-guide.mjs doctor
```

通过后按完整流程执行 `fixture`、`smoke`、`accuracy`、`threshold`、`soak`、`report`、`cleanup`：

- [本地容量压测操作手册](./local-capacity-runbook.md)
- [压力测试方案审计](./performance-audit.md)
- [容量报告模板](./report-template.md)

## 硬性规则

- 如果 verifier 不是 `PASS`，停止并修复问题，不得进入容量规划。
- 如果 runner `failed` 不是 `0`，停止并修复问题，不得进入容量规划。
- 如果 accuracy 命令不是 `PASS`，停止并修复问题，不得进入 threshold 或 soak。
- 如果 guide `report` 不是 `PASS`，不得发布吞吐、QPS、p95 或容量估算。
- `PASS_WITH_EXPECTED_FAILURES` 只允许用于故障注入报告。
- 不得在缺少对应 `perfRunId` 的情况下报告 QPS。
- 不得复用 `perfRunId`。
- cleanup 默认只清理 ledger 数据。完整清理必须显式使用 `--scope all --execute true`。
