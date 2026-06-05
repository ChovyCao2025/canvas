# Production Runtime Runbook

## Scope

This runbook covers the P0 production runtime gates for Marketing Canvas: rollback, degrade, kill, DLQ replay, delivery reconciliation, trace backlog response, Redis registry incidents, MySQL pressure, and incident handoff.

## Entry Checks

- Open `/ops` and confirm runtime status, current role, and latest audit events.
- Open Grafana dashboard `ops/grafana/canvas-runtime-dashboard.json`.
- Check Prometheus alert group `ops/alerts/canvas-runtime-rules.yml`.
- Confirm the operator has `TENANT_ADMIN`, `SUPER_ADMIN`, or legacy `ADMIN` before running emergency actions. `OPERATOR` is read-only.

## Failed Execution Spike

1. Confirm `CanvasFailedExecutionSpike` and identify affected tenant, canvas, and node type.
2. Pause or offline the affected canvas from `/ops` with a reason that references the incident ID.
3. Check recent deploy, template changes, provider availability, and policy changes.
4. Roll back the canvas if the latest version introduced the spike.
5. Verify the failure ratio returns below 5 percent for 10 minutes before resuming.

## DLQ Growth

1. Inspect DLQ records and group by canvas, trigger type, failed node, and tenant.
2. Pause the affected canvas if DLQ keeps growing.
3. Fix the root cause before replay: missing config, downstream outage, invalid payload, or route registry drift.
4. Replay only a small batch first and watch `canvas.capacity.dlq.backlog`.
5. Resume normal traffic after DLQ drains and no new records appear for 15 minutes.

## Delivery Outbox Dead Rows

1. Open the delivery monitor and filter `status=DEAD`.
2. Reconcile stale pending deliveries before replaying dead rows.
3. Replay a single dead row, confirm provider response and receipt callbacks, then replay the rest in batches.
4. Escalate to provider owner if DEAD rows return after replay.

## Trace Buffer Overflow

1. Check `canvas.trace.buffer.pending` and `canvas.trace.dropped.total`.
2. Reduce trace write load by pausing high-volume canvases or lowering trace sampling if configured.
3. Verify database write latency and queue depth.
4. Keep the incident open until dropped trace rate is zero for 5 minutes.

## Redis Registry Outage

1. Confirm Redis health and `canvas.redis.registry.latency`.
2. Run runtime route rebuild only after Redis connectivity is stable.
3. Invalidate cache for affected canvases if stale route data is suspected.
4. Verify trigger routes and scheduled jobs are rebuilt before resuming traffic.

## MySQL Pool Pressure

1. Check `canvas.mysql.pool.pressure.percent` and slow query dashboards.
2. Reduce load by pausing non-critical canvases or disabling high-volume triggers.
3. Confirm connection leaks, long transactions, and lock waits.
4. Resume traffic gradually after pressure stays below 70 percent for 10 minutes.

## Emergency Actions

- Pause/offline: stop new traffic for one canvas, then verify no new executions start.
- Kill: terminate running executions. Prefer `GRACEFUL`; use `FORCE` only when stuck executions block recovery.
- Rollback: restore the previous safe canvas version, then run a dry-run or canary before full resume.
- Resume: only after the alert is clear, root cause is documented, and audit events show the recovery action.

Every action must include a reason and produce an audit event.

## Incident Handoff

- Record incident ID, tenant, canvas IDs, start time, current state, and active mitigations.
- Attach dashboard screenshots or query links.
- List actions already taken and their audit IDs.
- Name the owner for root-cause analysis and the owner for customer communication.
