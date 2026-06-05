# P3-02 Service Decomposition Evidence

Date: 2026-06-05

## Verdict

P3-02 is implemented as a modular-monolith boundary-hardening package, not a physical service extraction. The current codebase has enough bounded-context shape to plan ownership and contracts, but it still has shared mapper access, one Flyway stream, direct runtime-to-CDP coupling, direct runtime-to-notification coupling, and emerging BI/warehouse modules inside the same deployable.

Physical extraction remains deferred by `docs/architecture/adr/ADR-0006-service-extraction-gate.md` and `docs/architecture/adr/ADR-0007-first-extraction-candidate.md`.

## Created Documents

- `docs/architecture/work-products/p3-02-service-boundaries/domain-map.md`
- `docs/architecture/work-products/p3-02-service-boundaries/domain-contract-inventory.md`
- `docs/architecture/adr/ADR-0007-first-extraction-candidate.md`

## Inventory Evidence

Commands run:

```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas -maxdepth 3 -type d | sort
find backend/canvas-engine/src/main/java/org/chovy/canvas/web -name '*Controller.java' -type f | wc -l
find backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper -maxdepth 1 -name '*Mapper.java' -type f | wc -l
find backend/canvas-engine/src/main/resources/db/migration -maxdepth 1 -type f \( -name 'V*.sql' -o -name 'beforeEachMigrate__*.sql' \) | wc -l
rg -l "CREATE TABLE" backend/canvas-engine/src/main/resources/db/migration | wc -l
rg -n "CREATE TABLE" backend/canvas-engine/src/main/resources/db/migration | wc -l
```

Observed results:

- 85 controllers.
- 146 shared mapper interfaces.
- 164 migration scripts.
- 85 migration files with `CREATE TABLE`.
- 172 `CREATE TABLE` statements.

## Boundary Decision

The first physical extraction candidate is `Deferred`.

The first executable slice is Reach / Notification boundary hardening inside the current monolith. This was chosen because notification, delivery outbox, delivery receipt, and provider response contracts can be wrapped behind ports and events without moving Canvas Authoring or Execution Runtime tables.

Canvas Authoring and Execution Runtime are explicitly not first candidates. They still share graph versioning, trigger admission, scheduler registration, Redis route/cache state, context persistence, execution lifecycle, and state-transition behavior.

## Contract Evidence

`docs/architecture/work-products/p3-02-service-boundaries/domain-contract-inventory.md` records:

- REST contracts for notification, delivery/receipt, CDP read-model dependency, and integration/provider dependency.
- Event and MQ contracts for notification realtime events, WebSocket tickets, delivery wakeups, runtime system alerts, provider responses, and cache invalidation.
- Redis key ownership for notification events, WebSocket tickets, event deduplication, trigger routes, and cache invalidation.
- Table ownership candidates for `notification`, `message_send_record`, `delivery_outbox`, `delivery_receipt_log`, customer/contactability state, marketing policy state, and channel connector state.
- Cross-domain call classification as synchronous API, event, read model, shared library, or prohibited coupling.
- A reversible strangler migration with old path, new path, proxy/adapter layer, dual-read, dual-write, compatibility window, rollback trigger, reconciliation, deployment order, tenant context, trace context, and idempotency.

## TestCompile Blockers Resolved

Targeted Maven verification initially could not reach the selected tests because existing test sources failed to compile. The blockers were fixed with minimal compatibility changes:

- Added `MarketingPolicyAdminController` for existing marketing policy tests.
- Added `MessageSendRecordController` for existing message-send record tests.
- Updated `CanvasExecutionRequestManagementControllerTest` to use `LambdaQueryWrapper#getSqlSegment()` instead of unavailable `getLastSql()`.
- Added compatibility overloads for BI `saveDraft(key, body)` calls while preserving the existing HTTP methods with lock-token headers.

These changes are compilation and compatibility fixes required for the repository's test suite to compile; they do not perform service extraction.

## Verification Commands

Backend characterization command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn test -pl canvas-engine -Dtest=CanvasUserQueryServiceTest,NotificationServiceTest,CanvasExecutionServiceCdpTest
```

Result: 16 tests run, 0 failures, 0 errors, 0 skipped.

Frontend API command:

```bash
cd frontend && npm test -- api
```

Result: 16 test files passed, 49 tests passed.

Documentation checks:

```bash
test -f docs/architecture/work-products/p3-02-service-boundaries/domain-map.md
rg "Canvas Authoring|Execution Runtime|CDP / Audience|Reach / Notification|Integration|Platform|Data Platform / Analytics|forbidden dependencies" docs/architecture/work-products/p3-02-service-boundaries/domain-map.md
test -f docs/architecture/adr/ADR-0007-first-extraction-candidate.md
rg "CDP / Audience|Reach / Notification|Integration / WeCom|coupling|data ownership|rollback|Deferred|ADR-0006" docs/architecture/work-products/p3-02-service-boundaries/domain-map.md docs/architecture/adr/ADR-0007-first-extraction-candidate.md
test -f docs/architecture/work-products/p3-02-service-boundaries/domain-contract-inventory.md
rg "REST|event|Redis key|MQ|table|DTO|tenant|synchronous API|read model|prohibited coupling|compatibility window" docs/architecture/work-products/p3-02-service-boundaries/domain-contract-inventory.md
rg "old path|new path|dual-read|dual-write|compatibility window|rollback trigger|reconciliation|tenant context|trace context|idempotency|ADR-0006" docs/architecture/work-products/p3-02-service-boundaries/domain-contract-inventory.md docs/architecture/adr/ADR-0007-first-extraction-candidate.md
```

Result: all documentation checks passed.

No files were staged or committed.
