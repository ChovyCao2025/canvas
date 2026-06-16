# Codex Code Review Prompt

Date: 2026-06-16

代码审核已经和 Browser E2E 端测合并到同一套模块窗口提示词中。

请使用：

- `docs/prompt/codex-browser-e2e-prompt.md`

每个模块窗口执行同一条链路：

1. 代码审核
2. Browser E2E 端测
3. 只修本模块内证据充分的问题
4. 最小验证
5. Browser 回归
6. 写入模块报告

模块报告统一写到：

- `docs/e2e-browser-audits/<module-slug>.md`

不要再单独启动一套 code-review-only 多窗口流程，避免和端测窗口重复审同一模块、重复写报告或抢改同一批文件。

