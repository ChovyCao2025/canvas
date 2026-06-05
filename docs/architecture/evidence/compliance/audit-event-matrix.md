# Audit Event Matrix

Status date: 2026-06-05

Audit events must identify tenant, actor, role, operation, target, request id, source IP, and masked metadata. The current storage boundary is `canvas_audit_log`; generic non-canvas targets use `canvas_id=0` until a future migration introduces a first-class generic audit table or relaxes the legacy `canvas_id NOT NULL` constraint.

| Operation family | Required event names | Target | Sensitive metadata | Current implementation |
| --- | --- | --- | --- | --- |
| Login and admin user changes | `login success`, `login failure`, `admin user create`, `admin user role change`, `admin user disable` | `auth-user` | username, role, IP, failure reason | Matrix defined; hook pending in auth services |
| Tenant changes | `tenant create`, `tenant disable`, `tenant activate`, `tenant quota update` | `tenant` | tenant id/key, quota summary, actor | `TenantController` records create/disable/activate audit events with masked metadata; quota update remains future product scope |
| Canvas lifecycle | `canvas create`, `canvas update`, `canvas publish`, `canvas offline`, `canvas archive`, `canvas delete`, `canvas kill` | `canvas` | graph summary, version ids, operator, IP | `CanvasController` records create/update/publish/offline/archive/kill/revert/canary/rollback/clone/safe-update audit events |
| Execution operations | `execution replay`, `execution dry-run`, `execution kill`, `execution retention run` | `execution` | execution id, canvas id, reason, actor | Matrix defined; replay/retention hooks pending |
| Data-source credential changes | `data-source credential create`, `data-source credential update`, `data-source credential delete`, `data-source credential test` | `data-source` | JDBC URL, username, password/token presence only | `DataSourceConfigController` records create/update/delete credential audit events; password remains encrypted and metadata stores presence only |
| Consent and suppression | `consent grant`, `consent opt-out`, `suppression add`, `suppression expire`, `suppression remove` | `marketing-policy` | user id, channel, reason, source | Policy tests added; mutation hooks pending |
| Deletion request | `deletion request intake`, `deletion dry-run`, `deletion execute`, `deletion tombstone`, `deletion rejected` | `data-subject-request` | user id, matched row counts, actor, legal-hold decision | `DataDeletionService` returns dry-run/execution counts including execution-trace matches; request-intake audit hook remains future product scope |
| Incident response | `incident declare`, `incident evidence export`, `legal hold apply`, `legal hold release` | `incident` | incident id, affected table families, owner | Checklist defined; incident workflow hook pending |

## Audit Payload Contract

`AuditEventService.AuditEventCommand` fields:

- `tenantId`
- `actor`
- `actorRole`
- `operation`
- `targetType`
- `targetId`
- `requestId`
- `ip`
- `fromVersion`
- `toVersion`
- `metadata`

`metadata` is passed through `PiiMaskingService.maskMetadata()` before JSON serialization, so values such as phone numbers, email addresses, open_id values, credentials, tokens, and secrets are not written raw.
