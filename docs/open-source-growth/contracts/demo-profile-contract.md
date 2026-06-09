# Demo Profile Contract

日期：2026-06-08

## Scope

Demo profile 用于开源首次体验和 Playground，不用于生产。

## Requirements

- 使用 mock provider。
- 自动导入官方插件和模板。
- 默认账号可登录。
- 不依赖真实短信、邮件、审批、优惠券、AI 服务。
- 支持 dry-run、trace 和 DSL 导出。

## Forbidden

- demo profile 配置进入 production profile。
- production profile 使用 mock provider 作为默认外部服务。
- demo 绕过执行引擎。
- demo 使用真实 secret。
- demo 关闭认证和租户边界。

## Golden Path

```text
docker compose -f docker-compose.demo.yml up
登录默认账号
导入 new-user-welcome 模板
dry-run
查看 trace
导出 DSL
CLI validate
AI 风险审计
```

## Verification

- `docker compose -f docker-compose.demo.yml config`
- 后端 demo 初始化测试。
- 前端 build。
- 手工 Golden Path 记录。

## Backend Placement / Owner

Current allowed state:

- `DOCS_ONLY` for quickstart, playground docs, mock catalog, and golden-path
  narrative.
- `CURRENT_ENGINE_BRIDGE` only when the worker packet names demo-only config,
  seed service entry point, final DDD owner, idempotency behavior, and bridge
  removal gate.
- `DDD_FINAL_MODULE` after `canvas-boot` owns runtime profile assembly and
  `canvas-context-canvas` / `canvas-context-execution` expose supported demo
  seed, dry-run, trace, and DSL export APIs.

Final owner:

- Demo runtime profile and config wiring: `canvas-boot`.
- Demo canvas/template seed through public draft APIs: `canvas-context-canvas`.
- Demo dry-run and trace: `canvas-context-execution`.
- Public docs and playground guide: `docs/open-source/**`.

Allowed adapters:

- `application-demo.yml` and demo-only compose files.
- Mock providers only; no production profile defaults.
- Canvas/execution APIs only; no direct database seeding unless a coordinator
  writes a named bridge with a removal gate.

Mirror documents:

- `docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md`
- `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md`
- `docs/program-coordination/execution-readiness-audit.md`

Verification:

- Demo config does not alter production or staging defaults.
- Golden Path uses the same execution, trace, permission, and tenant boundaries
  as non-demo flows.
- `docker compose -f docker-compose.demo.yml config` passes before release.
