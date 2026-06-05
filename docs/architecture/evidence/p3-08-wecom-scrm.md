# P3-08 WeCom SCRM Evidence

Date: 2026-06-05

## Verdict

WeCom implementation remains deferred, but the first candidate slice is defined as connector configuration plus callback ingestion inside the Integration bounded context. Full customer sync, group operations, journey send node, session tracking, and service extraction are not approved.

## Created Documents

- `docs/architecture/wecom-scrm-implementation-slice.md`
- `docs/architecture/wecom-scrm-integration-boundary.md`
- `docs/architecture/wecom-scrm-test-plan.md`

## Inventory Commands

```bash
rg -n "WeCom|wecom|企微|企业微信|ChannelConnector|channel connector|callback|signature|replay|SCRM|scrm" backend/canvas-engine/src/main/java backend/canvas-engine/src/test/java frontend/src docs/architecture
find backend/canvas-engine/src/main/java/org/chovy/canvas -iname '*channel*' -o -iname '*connector*' -o -iname '*wecom*' | sort
find frontend/src -iname '*channel*' -o -iname '*connector*' -o -iname '*wecom*' | sort
```

## Current Findings

- Existing code has generic channel connector backend tables, mappers, controller, service contracts, and frontend service/tests.
- Existing code does not have a dedicated WeCom adapter, callback endpoint, callback event ledger, or WeCom frontend configuration.
- P3-00 and ADR-0006 require WeCom to start inside Integration context and forbid premature service extraction.
- Current channel connector contract can be reused for provider mode, health, and future send/receipt abstraction.

## Decision Summary

- First candidate slice: WeCom connector configuration plus callback ingestion.
- Implementation status: Deferred until product, compliance, security, Integration, and operations owners are named and sandbox credentials are available outside git.
- Generic DAG handlers must avoid direct WeCom client dependencies by using `ChannelConnector` or a delivery command port.

## Verification Commands

```bash
test -f docs/architecture/wecom-scrm-implementation-slice.md
rg -n "Scope|Out of scope|API|Data|Consent|Compliance|Callback|Rollback|Owner|Deferred" docs/architecture/wecom-scrm-implementation-slice.md
test -f docs/architecture/wecom-scrm-integration-boundary.md
rg -n "adapter|domain service|handler contract|callback|frontend API|signature|replay|idempotency|retry|DLQ|reconciliation|generic DAG" docs/architecture/wecom-scrm-integration-boundary.md
test -f docs/architecture/wecom-scrm-test-plan.md
rg -n "signature|replay|idempotency|retry|adapter failure|DLQ|handler output|form payload|disabled state|sandbox|secrets" docs/architecture/wecom-scrm-test-plan.md
```

Result: all documentation checks passed.

## Follow-ups

- Name owners and create a sandbox secret handling procedure.
- Add backend callback security/idempotency tests before implementation.
- Add frontend typed API and disabled-state tests before exposing UI.
- Keep WeCom service extraction blocked by ADR-0006 until callback, sync, credential, tenant, compliance, and operations evidence exists.

No P3 files were staged or committed by this task.
