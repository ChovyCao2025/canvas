# Canvas Retention Policy

Date: 2026-06-05

## MySQL Retention Rules

| Table or family | Online retention | Action after window | Compliance handling | Owner |
| --- | --- | --- | --- | --- |
| `canvas_execution` | 180 days | archive terminal rows, then delete online copy | legal hold supported through `execution_retention_policy` | Runtime platform |
| `canvas_execution_trace` | 30 days | archive incident window traces, then delete online copy | no long-term PII retention unless incident evidence requires it | Runtime platform |
| `canvas_execution_dlq` | 90 days after resolution | delete after replay/incident closure | retain incident summary separately | Runtime platform |
| `canvas_execution_request` | 90 days | compact success payloads, archive idempotency ledger, then delete | keep failed/replayed evidence until incident close | Runtime platform |
| `canvas_execution_stats` | 730 days | keep aggregate rows online | aggregate only; no raw payload | Data platform |
| audit logs | 365 days minimum | keep compliance evidence, archive older records | legal hold overrides deletion | Compliance owner |
| `event_log` | 30 days online | archive to CDP warehouse, then delete online copy | raw payload retention follows CDP policy | CDP platform |
| quota ledgers | valid-end plus 7 days | delete stale DB/Redis quota markers | keep aggregate quota violations in audit | Runtime platform |

`V239__execution_retention_policy.sql` creates `execution_retention_policy`, `execution_retention_run`, and `execution_retention_archive_manifest` so cleanup jobs can record cutoff, action, row counts, status, archive target, and errors.

## Redis TTL Rules

| Key family | Constructor or pattern | TTL rule | Action | Owner |
| --- | --- | --- | --- | --- |
| context | `RedisKeyUtil.context(canvasId, userId)` | `canvas.execution.context-ttl-sec`, default 86,400 seconds | expire automatically | Runtime platform |
| dedup | `RedisKeyUtil.dedup(canvasId, userId, msgId)` | at least retry window plus 24 hours | expire automatically | Runtime platform |
| event dedup | `RedisKeyUtil.eventDedup(idempotencyKey)` | 24 hours | expire automatically | CDP platform |
| quota | `RedisKeyUtil.quota(canvasId, userId, date)` | next day plus 7 days | expire automatically; reconcile with inactive canvas cleanup | Runtime platform |
| global quota | `RedisKeyUtil.globalCount(canvasId)` | canvas valid end plus 7 days, or manual reconciliation for permanent canvases | delete during offline/archive/kill reconciliation | Runtime platform |
| route keys | `triggerMq`, `triggerBehavior`, `triggerTagger` | no TTL while published | rebuild or delete on publish/offline/archive/kill | Runtime platform |
| cache invalidation | `cacheInvalidateChannel` | pub/sub channel, no retained value | no cleanup required | Runtime platform |
| kill switch | `killChannel`, `killPattern` | no TTL for active kill command window unless implementation sets ephemeral payload | clear when canvas lifecycle reaches terminal state | Runtime platform |
| websocket tickets | `notificationWsTicket(ticket)` | <= 5 minutes | expire automatically | Notification owner |

## Cleanup Safety

- Cleanup jobs must run in dry-run mode first and write an `execution_retention_run` row with `status=DRY_RUN`.
- Archive actions must write `execution_retention_archive_manifest` before deleting online rows.
- Delete actions must be idempotent by cutoff and table name.
- Legal hold disables deletion for affected table families until compliance clears the hold.
- Capacity or compliance reports must cite the corresponding retention run id.
