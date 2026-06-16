# Codex Module Review + Browser E2E Audit

Date: 2026-06-16

## 用法

代码审核和 Browser E2E 端测使用同一套模块窗口。每个 Codex 窗口只复制 `docs/prompt/codex-browser-e2e-prompt.md` 中的一个 `Goal:` 块。

所有窗口直接在当前本地分支和当前工作区执行：

- 不创建 git worktree
- 不创建新分支
- 不要求用户手动 `cd`
- 不回滚用户已有改动

每个模块窗口执行固定链路：

1. 审核本模块代码。
2. 用 Browser 测本模块页面。
3. 记录发现的问题。
4. 只修本模块范围内、证据充分的问题。
5. 运行最小必要验证。
6. 用 Browser 回归对应页面。
7. 把审核、端测、修复、验证写入本模块报告。

## Prompt

- `docs/prompt/codex-browser-e2e-prompt.md`

`docs/prompt/codex-code-review-prompt.md` 只保留为兼容入口，实际也指向同一套合并提示词。

## Module Windows

| Module | Report | Routes |
| --- | --- | --- |
| public-auth | `docs/e2e-browser-audits/public-auth.md` | `/login`, `/public/forms/:publicKey`, `/bi/embed/:resourceType/:resourceKey` |
| home-ops-collaboration | `docs/e2e-browser-audits/home-ops-collaboration.md` | `/home`, `/ops`, `/approvals`, `/conversations` |
| canvas-journey | `docs/e2e-browser-audits/canvas-journey.md` | `/canvas`, `/canvas/:id/edit`, `/canvas/:id/stats`, `/canvas/:id/users` |
| marketing-suite | `docs/e2e-browser-audits/marketing-suite.md` | `/marketing-monitoring`, `/mautic-insights`, `/marketing-platform`, `/search-marketing`, `/risk`, `/growth-activities`, `/marketing-preferences`, `/marketing-forms`, `/content-hub`, `/message-templates`, `/message-deliveries`, `/channel-connectors`, `/demo-sandbox` |
| bi-analytics | `docs/e2e-browser-audits/bi-analytics.md` | `/bi`, `/analytics` |
| cdp-audience | `docs/e2e-browser-audits/cdp-audience.md` | `/cdp/users`, `/cdp/users/:userId`, `/audiences`, `/audiences/new`, `/audiences/:id/edit`, `/cdp/computed-profile`, `/cdp/computed-tags`, `/cdp/realtime-audiences` |
| admin-config | `docs/e2e-browser-audits/admin-config.md` | `/admin/users`, `/admin/projects`, `/api-config`, `/data-source-config`, `/ab-experiments`, `/tag-config`, `/identity-types`, `/tag-import`, `/mq-config`, `/event-config`, `/webhook-subscriptions`, `/api-docs`, `/system-options`, `/test-users`, `/ai-predictions` |
| tenant-admin | `docs/e2e-browser-audits/tenant-admin.md` | `/admin/tenants` |

## Isolation Rules

1. 每个窗口只写自己的 `docs/e2e-browser-audits/<module-slug>.md`。
2. 每个窗口只写自己的 `tmp/module-quality-<module-slug>-progress.md`。
3. 不同窗口不要同时修改同一个源码文件。
4. 当前窗口需要改共享文件时，先在报告中标记 `needs coordination`。
5. 一个路由状态为 `passed` 后，除非本模块修复可能影响它，否则不要重复测试。
6. 一个路由状态为 `blocked` 后，只有缺少的账号、权限、测试数据、服务或环境补齐后才重试。

## Status Values

- `pending`: not reviewed or tested yet
- `reviewing`: code review in progress
- `testing`: Browser test in progress or interrupted
- `passed`: route passed Browser checks
- `failed`: bug found and not yet fixed
- `fixed`: bug found, fixed, verified, and Browser-regressed
- `blocked`: missing account, role, service, test data, or environment
- `needs coordination`: issue belongs to another module or requires shared-file ownership

